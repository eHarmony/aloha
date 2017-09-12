package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.File

import com.eharmony.aloha.audit.impl.tree.{RootedTree, RootedTreeAuditor}
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.sources.{ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.models.multilabel.MultilabelModel
import com.eharmony.aloha.semantics.func.GenFunc0
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}

import scala.annotation.tailrec

/**
  * Created by ryan.deak on 9/11/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwMultilabelModelTest {
  import VwMultilabelModelTest._

  @Test def test1(): Unit = {
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

  private lazy val Model: Model[Domain, RootedTree[Any, Map[Label, Double]]] = {
    val featureNames = Vector(FeatureName)
    val features = Vector(GenFunc0("", (_: Domain) => Iterable(("", 1d))))

    // Get the list of labels from the comma-separated list passed in the input string.
    val labelsOfInterestFn =
      GenFunc0("",
        (x: Domain) =>
          x.split("\\s*,\\s*")
           .map(label => label.trim)
           .filter(label => label.nonEmpty)
           .toVector
      )

    val predProd = VwSparseMultilabelPredictorProducer[Label](
      modelSource = TrainedModel,
      params      = "", // to see the output:  "-p /dev/stdout",
      defaultNs   = List.empty[Int],
      namespaces  = List(("X", List(0)))
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
      """
        | --quiet
        | --csoaa_ldf mc
        | --csoaa_rank
        | --loss_function logistic
        | -q YX
        | --noconstant
        | --ignore_linear X
        | --ignore y
        | -f
      """.stripMargin.trim

    (flags + " " + modelFile.getCanonicalPath).split("\n").map(_.trim).mkString(" ")
  }

  private val FeatureName = "feature"

  /**
    * A dataset that creates the following marginal distribution.
    - Pr[seven] = 0.7  where seven is _C0_
    - Pr[eight] = 0.8  where eight is _C1_
    - Pr[six]   = 0.6  where six   is _C2_
    *
    * The observant reader may notice these are oddly ordered.  On each line C1 appears first,
    * then C0, then C2.  This is done to show ordering doesn't matter.  What matters is the
    * class '''indices'''.
    */
  private val TrainingData =
    Vector(
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.084 |y _C2147483649_\n1:0.0 |Y _C1_\n0:-0.084 |Y _C0_\n2:-0.084 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.024 |y _C2147483649_\n1:0.0 |Y _C1_\n0:0.0 |Y _C0_\n2:0.0 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.336 |y _C2147483649_\n1:-0.336 |Y _C1_\n0:-0.336 |Y _C0_\n2:-0.336 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.056 |y _C2147483649_\n1:0.0 |Y _C1_\n0:-0.056 |Y _C0_\n2:0.0 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.144 |y _C2147483649_\n1:-0.144 |Y _C1_\n0:0.0 |Y _C0_\n2:-0.144 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.224 |y _C2147483649_\n1:-0.224 |Y _C1_\n0:-0.224 |Y _C0_\n2:0.0 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.036 |y _C2147483649_\n1:0.0 |Y _C1_\n0:0.0 |Y _C0_\n2:-0.036 |Y _C2_",
      s"shared |X $FeatureName\n2147483648:0.0 |y _C2147483648_\n2147483649:-0.096 |y _C2147483649_\n1:-0.096 |Y _C1_\n0:0.0 |Y _C0_\n2:0.0 |Y _C2_"
    ).map(_.split(raw"\n"))

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