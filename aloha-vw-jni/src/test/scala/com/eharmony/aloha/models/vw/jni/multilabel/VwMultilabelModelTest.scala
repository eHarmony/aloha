package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.File

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.audit.impl.tree.{RootedTree, RootedTreeAuditor}
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator.LabelNamespaces
import com.eharmony.aloha.dataset.vw.multilabel.json.VwMultilabeledJson
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.sources.{ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.models.multilabel.MultilabelModel
import com.eharmony.aloha.semantics.compiled.CompiledSemanticsInstances
import com.eharmony.aloha.semantics.func.GenFunc0
import org.apache.commons.vfs2.VFS
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol.{IntJsonFormat, StringJsonFormat}
import spray.json.{DefaultJsonProtocol, JsonWriter, RootJsonFormat, pimpString}
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}

import scala.annotation.tailrec
import scala.util.Random

/**
  * Created by ryan.deak on 9/11/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwMultilabelModelTest {
  import VwMultilabelModelTest._

  @Test def testExpectedUnrecoverableFlags(): Unit = {
    assertEquals(
      "Unrecoverable flags has changed.",
      Set("cubic", "redefine", "stage_poly", "ignore", "ignore_linear", "keep"),
      VwMultilabelModel.UnrecoverableFlagSet
    )
  }

  @Test def testUnrecoverable(): Unit = {
    val unrec = VwMultilabelModel.UnrecoverableFlagSet.iterator.map { f =>
      VwMultilabelModel.updatedVwParams(s"--$f", Set.empty)
    }

    unrec foreach {
      case Left(UnrecoverableParams(p, us)) => assertEquals(p, us.map(u => s"--$u").mkString(" "))
      case _                                => fail()
    }
  }

  @Test def testQuadraticCreation(): Unit = {
    val args = "--csoaa_ldf mc"

    // Notice: ignore_linear and quadratics are in sorted order.
    val exp  = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
               "--ignore_linear a --ignore_linear b -qYa -qYb"
    VwMultilabelModel.updatedVwParams(args, Set("abc", "bcd")) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  @Test def testCubicCreation(): Unit = {
    val args = "--csoaa_ldf mc -qab --quadratic cb"
    val exp = "--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
              "--ignore_linear a --ignore_linear b --ignore_linear c " +
              "-qYa -qYb -qYc " +
              "--cubic Yab --cubic Ybc"

    // Notice: ignore_linear and quadratics are in sorted order.
    VwMultilabelModel.updatedVwParams(args, Set("abc", "bcd", "cde")) match {
      case Right(s) => assertEquals(exp, s)
      case _ => fail()
    }
  }

  // TODO: More VW argument augmentation tests!!!



  @Test def testTrainedModelWorks(): Unit = {
    val model = Model

    try {
      // Test the no label case.
      testEmpty(model(""))

      // Test all elements of the power set of labels (except the empty set, done above).
      for {
        labels <- powerSet(AllLabels.toSet).filter(ls => ls.nonEmpty)
        x       = labels.mkString(",")
        y       = model(x)
      } testOutput(y, labels, ExpectedMarginalDist)
    }
    finally {
      model.close()
    }
  }

  @Test def testTrainedModelCanBeParsedAndUsed(): Unit =
    testModel(None)

  @Test def testTrainedModelCanBeParsedAndUsedWithLabels67(): Unit =
    testModel(Option(Set(LabelSix, LabelSeven)))

  @Test def testTrainedModelCanBeParsedAndUsedWithLabels678(): Unit =
    testModel(Option(Set(LabelSix, LabelSeven, LabelEight)))

  @Test def testTrainedModelCanBeParsedAndUsedWithNoLabels(): Unit =
    testModel(Option(Set.empty), modelShouldProduceOutput = false)

  // This is really more of an integration test.
  @Test def testTrainingAndTesting(): Unit = {

    // ------------------------------------------------------------------------------------
    //  Preliminaries
    // ------------------------------------------------------------------------------------

    type Lab = Int
    type Dom = Vector[Lab]  // Not an abuse of notation.  Domain is indeed a vector of labels.

    val semantics = CompiledSemanticsInstances.anyNameIdentitySemantics[Dom]
    val optAud = OptionAuditor[Map[Lab, Double]]()

    // ------------------------------------------------------------------------------------
    //  Dataset and test example set up.
    // ------------------------------------------------------------------------------------

    // Marginal Distribution:          Pr[8] = 0.80 = 20 / 25 = (12 + 8) / 25
    //                                 Pr[4] = 0.40 = 10 / 25 = ( 2 + 8) / 25
    //
    val unshuffledTrainingSet: Seq[Dom] = Seq(
      Vector(    )  -> 3,   //            pr = 0.12 =  3 / 25   <-- JPD
      Vector(   8)  -> 12,  //            pr = 0.48 = 12 / 25
      Vector(4   )  -> 2,   //            pr = 0.08 =  2 / 25
      Vector(4, 8)  -> 8    //            pr = 0.32 =  8 / 25
    ) flatMap {
      case (k, n) => Vector.fill(n)(k)
    }

    val trainingSet = new Random(0).shuffle(unshuffledTrainingSet)

    val labelsInTrainingSet = trainingSet.flatten.toSet.toVector.sorted

    val testExample: Dom = Vector.empty

    val marginalDist = labelsInTrainingSet.map { label =>
      val z = trainingSet.size.toDouble
      label -> trainingSet.count(row => row contains label) / z
    }.toMap

    // ------------------------------------------------------------------------------------
    //  Prepare dataset specification and read training dataset.
    // ------------------------------------------------------------------------------------

    val datasetSpec = VFS.getManager.resolveFile(EndToEndDatasetSpec)
    val datasetJson = StringReadable.fromVfs2(datasetSpec).parseJson.convertTo[VwMultilabeledJson]

    val rc =
      new VwMultilabelRowCreator.Producer[Dom, Lab](labelsInTrainingSet).
        getRowCreator(semantics, datasetJson).get

    // ------------------------------------------------------------------------------------
    //  Prepare parameters for VW model that will be trained.
    // ------------------------------------------------------------------------------------


    val binaryVwModel = File.createTempFile("vw_", ".bin.model")
    binaryVwModel.deleteOnExit()

    val cacheFile = File.createTempFile("vw_", ".cache")
    cacheFile.deleteOnExit()

    // probs / costs
    //   --csoaa_ldf mc  vs  m
    //   --csoaa_rank
    //   --loss_function logistic  add this
    //   --noconstant
    //   --ignore_linear X
    //   --ignore $dummyLabelNs
    //  turn first order features into quadratics
    //  turn quadratic features into cubics
    //
    val vwParams =
      s"""
         | --quiet
         | --csoaa_ldf mc
         | --csoaa_rank
         | --loss_function logistic
         | -q ${labelNs}X
         | --noconstant
         | --ignore_linear X
         | --ignore $dummyLabelNs
         | -f ${binaryVwModel.getCanonicalPath}
         | --passes 50
         | --cache_file ${cacheFile.getCanonicalPath}
         | --holdout_off
         | --learning_rate 5
         | --decay_learning_rate 0.9
       """.stripMargin.trim.replaceAll("\n", " ")


    // ------------------------------------------------------------------------------------
    //  Train VW model
    // ------------------------------------------------------------------------------------

    val vwLearner = VWLearners.create[VWActionScoresLearner](vwParams)
    trainingSet foreach { row =>
      val x = rc(row)._2
      vwLearner.learn(x)
    }
    vwLearner.close()

    // ------------------------------------------------------------------------------------
    //  Create Aloha model JSON
    // ------------------------------------------------------------------------------------

    val modelJson = VwMultilabelModel.json(
      datasetSpec = Vfs.apacheVfs2ToAloha(datasetSpec),
      binaryVwModel = Vfs.javaFileToAloha(binaryVwModel),
      id = ModelId(1, "NONE"),
      labelsInTrainingSet = labelsInTrainingSet,
      labelsOfInterest = Option.empty[String],
      vwArgs = Option.empty[String],
      externalModel = false,
      numMissingThreshold = Option(0)
    )


    // ------------------------------------------------------------------------------------
    //  Instantiate Aloha Model
    // ------------------------------------------------------------------------------------

    val factory = ModelFactory.defaultFactory(semantics, optAud)
    val modelTry = factory.fromString(modelJson.prettyPrint) // Use `.compactPrint` in prod.
    val model = modelTry.get

    // ------------------------------------------------------------------------------------
    //  Test Aloha Model
    // ------------------------------------------------------------------------------------

    val output = model(testExample)
    model.close()

    output match {
      case None => fail()
      case Some(m) =>
        assertEquals(marginalDist.keySet, m.keySet)
        marginalDist foreach { case (k, v) =>
          assertEquals(s"For key '$k':", v, m(k), 0.01)
        }
    }
  }

  /**
    *
    * @param desiredLabels Notice since this is a Set, label order doesn't matter.
    * @param modelShouldProduceOutput whether the model is expected to
    */
  private[this] def testModel(
      desiredLabels: Option[Set[Label]],
      modelShouldProduceOutput: Boolean = true
  ): Unit = {

    val factory =
      ModelFactory.defaultFactory(
        CompiledSemanticsInstances.anyNameIdentitySemantics[Set[Label]],
        OptionAuditor[Map[Label, Double]]()
      )

    // "${desired_labels_from_input}.toVector" returns the input passed to the model in
    // the form of a Vector.  See anyNameIdentitySemantics for more information.
    val desiredLabelFn = desiredLabels map (_ => "${desired_labels_from_input}.toVector")
    val json = modelJson(TrainedModel, AllLabels, desiredLabelFn)

    val modelTry = factory.fromString(json)
    val model = modelTry.get  // : Model[Set[Label], Option[Map[Label, Double]]]
    val x = desiredLabels getOrElse Set.empty
    val y = model(x)
    model.close()

    y match {
      case None =>
        if (modelShouldProduceOutput)
          fail("Model should produce output.  Produced None.")
      case Some(m) =>
        if (!modelShouldProduceOutput)
          fail(s"Model should not produce output.  Produced: $m")
        else {
          val expected = desiredLabels match {
            case None         => ExpectedMarginalDist
            case Some(labels) => ExpectedMarginalDist.filterKeys(labels.contains)
          }

          // The keys should be the same set, not a super set.
          assertEquals(expected.keySet, m.keySet)

          // Test the value associated with the label.
          expected foreach { case (k, v) =>
            assertEquals(s"For key '$k':", v, m(k), 0.01)
          }
        }
    }
  }


  private[this] def testEmpty(yEmpty: PredictionOutput): Unit = {
    assertEquals(None, yEmpty.value)
    assertEquals(Vector("No labels provided. Cannot produce a prediction."), yEmpty.errorMsgs)
    assertEquals(Set.empty, yEmpty.missingVarNames)
    assertEquals(None, yEmpty.prob)
  }

  private[this] def testOutput(
      y: PredictionOutput,
      labels: Set[Label],
      expectedMarginalDist: Map[Label, Double]
  ): Unit = y.value match {
    case None => fail()
    case Some(labelMap) =>
      // Should that model gives back all the keys that were request.
      assertEquals(labels, labelMap.keySet)

      // The expected return value.  We check keys first to make sure the model
      // doesn't return extraneous key-value pairs.
      val exp = expectedMarginalDist.filterKeys(label => labels contains label)

      // Check that the values returned are correct.
      exp foreach { case (label, expPr) =>
        assertEquals(expPr, labelMap(label), 0.01)
      }
  }
}

object VwMultilabelModelTest {
  private type Label  = String
  private type Domain = String
  private type PredictionOutput = RootedTree[Any, Map[Label, Double]]

  private[this] val TrainingEpochs = 30

  private val LabelSix   = "six"
  private val LabelSeven = "seven"
  private val LabelEight = "eight"

  private val ExpectedMarginalDist = Map(
    LabelSix   -> 0.6,
    LabelSeven -> 0.7,
    LabelEight -> 0.8
  )

  /**
    * The order here is important because it's the same as the indices in the training data.
    - LabelSeven is _C0_ (index 0)
    - LabelEight is _C1_ (index 1)
    - LabelSix   is _C2_ (index 2)
    */
  private val AllLabels = Vector(LabelSeven, LabelEight, LabelSix)

  private val EndToEndDatasetSpec =
    "res:com/eharmony/aloha/models/vw/jni/multilabel/dataset_spec.json"

  private lazy val Model: Model[Domain, RootedTree[Any, Map[Label, Double]]] = {
    val featureNames = Vector(FeatureName)
    val features     = Vector(GenFunc0("", (_: Domain) => Iterable(("", 1d))))

    // Get the list of labels from the comma-separated list passed in the input string.
    val labelsOfInterestFn =
      GenFunc0("",
        (x: Domain) =>
          x.split("\\s*,\\s*")
           .map(label => label.trim)
           .filter(label => label.nonEmpty)
           .toVector
      )

    val namespaces = List(("X", List(0)))
    val labelNs = VwMultilabelRowCreator.determineLabelNamespaces(namespaces.unzip._1.toSet).get.labelNs


    val predProd = VwSparseMultilabelPredictorProducer[Label](
      modelSource = TrainedModel,
      params      = "", // to see the output:  "-p /dev/stdout",
      defaultNs   = List.empty[Int],
      namespaces  = namespaces,
      labelNamespace = labelNs,
      numLabelsInTrainingSet = AllLabels.size
    )

    MultilabelModel(
      modelId             = ModelId(1, "model"),
      featureNames        = featureNames,
      featureFunctions    = features,
      labelsInTrainingSet = AllLabels,
      labelsOfInterest    = Option(labelsOfInterestFn),
      predictorProducer   = predProd,
      numMissingThreshold = Option.empty[Int],
      auditor             = Auditor)
  }

  private def tmpFile() = {
    val f = File.createTempFile(classOf[VwMultilabelModelTest].getSimpleName + "_", ".vw.model")
    f.deleteOnExit()
    f
  }

  private val LabelNamespaces(labelNs, dummyLabelNs) =
    VwMultilabelRowCreator.determineLabelNamespaces(Set.empty).get

  private def vwTrainingParams(modelFile: File = tmpFile()) = {

    // NOTES:
    //  1. `--csoaa_rank`  is needed by VW to make a VWActionScoresLearner.
    //  2. `-q YX`  forms the cross product of features between the label-based features in Y
    //     and the side information in X.  If the features in namespace Y are unique to each
    //     class, the cross product effectively makes one model per class by interacting the
    //     class features to the features in X.  Since the class labels vary (possibly)
    //     independently, this cross product is capable of creating independent models
    //     (one per class).
    //  3. `--ignore_linear Y`  is provided because in the data, there is one constant feature
    //     in the Y namespace per class.  This flag acts essentially the same as the
    //     `--noconstant`  flag in the traditional binary classification context.  It omits the
    //     one feature related to the class (the intercept).
    //  4. `--noconstant`  is specified because there's no need to have a "common intercept"
    //     whose value is the same across all models.  This should be the job of the
    //     first-order features in Y (the per-class intercepts).
    //  5. `--ignore y`  is used to ignore the features in the namespace related to the dummy
    //     classes.  We need these two dummy classes in training to make  `--csoaa_ldf mc`
    //     output proper probabilities.
    //  6. `--link logistic`  doesn't actually seem to do anything.
    //  7. `--loss_function logistic`  works properly during training; however, when
    //     interrogating the scores, it is important to remember they appear as
    //     '''negative logits'''.  This is because the CSOAA algorithm uses '''costs''', so
    //     "smaller is better".  So, to get the probability, one must do `1/(1 + exp(-1 * -y))`
    //     or simply `1/(1 + exp(y))`.
    val flags =
      s"""
         | --quiet
         | --csoaa_ldf mc
         | --csoaa_rank
         | --loss_function logistic
         | -q ${labelNs}X
         | --noconstant
         | --ignore_linear X
         | --ignore $dummyLabelNs
         | -f
       """.stripMargin.trim

    (flags + " " + modelFile.getCanonicalPath).split("\n").map(_.trim).mkString(" ")
  }

  private val FeatureName = "feature"

  /**
    * A dataset that creates the following marginal distribution.
    - Pr[seven] = 0.7  where seven is _0
    - Pr[eight] = 0.8  where eight is _1
    - Pr[six]   = 0.6  where six   is _2
    *
    * The observant reader may notice these are oddly ordered.  On each line _1 appears first,
    * then _0, then _2.  This is done to show ordering doesn't matter.  What matters is the
    * class '''indices'''.
    */
  private val TrainingData =

    Vector(
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.084 |$dummyLabelNs pos\n1:0.0    |$labelNs _1\n0:-0.084 |$labelNs _0\n2:-0.084 |$labelNs _2",
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.024 |$dummyLabelNs pos\n1:0.0    |$labelNs _1\n0:0.0    |$labelNs _0\n2:0.0    |$labelNs _2",
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.336 |$dummyLabelNs pos\n1:-0.336 |$labelNs _1\n0:-0.336 |$labelNs _0\n2:-0.336 |$labelNs _2",
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.056 |$dummyLabelNs pos\n1:0.0    |$labelNs _1\n0:-0.056 |$labelNs _0\n2:0.0    |$labelNs _2",
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.144 |$dummyLabelNs pos\n1:-0.144 |$labelNs _1\n0:0.0    |$labelNs _0\n2:-0.144 |$labelNs _2",
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.224 |$dummyLabelNs pos\n1:-0.224 |$labelNs _1\n0:-0.224 |$labelNs _0\n2:0.0    |$labelNs _2",
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.036 |$dummyLabelNs pos\n1:0.0    |$labelNs _1\n0:0.0    |$labelNs _0\n2:-0.036 |$labelNs _2",
      s"shared |X $FeatureName\n2147483648:0.0 |$dummyLabelNs neg\n2147483649:-0.096 |$dummyLabelNs pos\n1:-0.096 |$labelNs _1\n0:0.0    |$labelNs _0\n2:0.0    |$labelNs _2"
    ).map(_.split("\n"))

  private lazy val TrainedModel: ModelSource = {
    val modelFile = tmpFile()
    val params = vwTrainingParams(modelFile)
    val learner = VWLearners.create[VWActionScoresLearner](params)
    for {
      _ <- 1 to TrainingEpochs
      d <- TrainingData
    } learner.learn(d)

    learner.close()

    ExternalSource(Vfs.javaFileToAloha(modelFile))
  }

  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]]()

  private implicit def vecWriter[K: JsonWriter]: RootJsonFormat[Vector[K]] =
    DefaultJsonProtocol.vectorFormat(DefaultJsonProtocol.lift(implicitly[JsonWriter[K]]))

  private[multilabel] def modelJson[K: JsonWriter](
      modelSource: ModelSource,
      labelsInTrainingSet: Vector[K],
      labelsOfInterest: Option[String] = None) = {

//    implicit val vecWriter = vectorFormat(DefaultJsonProtocol.lift(implicitly[JsonWriter[K]]))

    val loi = labelsOfInterest.fold(""){ f =>
      val escaped = f.replaceAll("\"", "\\\"")
      s""""labelsOfInterest": "$escaped",\n"""
    }

    val json =
      s"""
         |{
         |  "modelType": "multilabel-sparse",
         |  "modelId": { "id": 1, "name": "NONE" },
         |  "features": {
         |    "feature": "1"
         |  },
         |  "numMissingThreshold": 0,
         |  "labelsInTrainingSet": ${toJsonString(labelsInTrainingSet)},
         |$loi
         |  "underlying": {
         |    "type": "vw",
         |    "modelSource": ${toJsonString(modelSource)},
         |    "namespaces": {
         |      "X": [
         |        "feature"
         |      ]
         |    }
         |  }
         |}
       """.stripMargin.trim
    json
  }

  private def toJsonString[A: JsonWriter](a: A): String =
    implicitly[JsonWriter[A]].write(a).compactPrint


  /**
    * Creates the power set of the provided set.
    * Answer provided by Chris Marshall (@oxbow_lakes) on
    * [[https://stackoverflow.com/a/11581323 Stack Overflow]].
    * @param generatorSet a set for which a power set should be produced.
    * @tparam A type of elements in the set.
    * @return the power set of `generatorSet`.
    */
  private def powerSet[A](generatorSet: Set[A]): Set[Set[A]] = {
    @tailrec def pwr(t: Set[A], ps: Set[Set[A]]): Set[Set[A]] =
      if (t.isEmpty) ps
      else pwr(t.tail, ps ++ (ps map (_ + t.head)))

    // powerset of empty set is the set of the empty set.
    pwr(generatorSet, Set(Set.empty[A]))
  }
}
