package com.eharmony.aloha.models.multilabel

import java.io.{Closeable, PrintWriter, StringWriter}

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.tree.RootedTreeAuditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.semantics.SemanticsUdfException
import com.eharmony.aloha.semantics.func._
import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.Try

/**
  * Created by ryan.deak on 9/1/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class MultilabelModelTest extends ModelSerializationTestHelper {
  import MultilabelModel._
  import MultilabelModelTest._

  @Test def testSerialization(): Unit = {
    // Assuming all parameters passed to the MultilabelModel constructor are
    // Serializable, MultilabelModel should also be Serializable.
    val modelRoundTrip = serializeDeserializeRoundTrip(modelNoFeatures)
    assertEquals(modelNoFeatures, modelRoundTrip)
  }

  @Test def testModelCloseClosesPredictor(): Unit = {
    // Make the predictorProducer passed to the constructor be a
    //   'SparsePredictorProducer[K] with Closeable'.
    // predictorProducer should track whether it is closed (using an AtomicBoolean or something).
    // Call close on the MultilabelModel instance and ensure that the underlying predictor is
    // also closed.

    case class ConstantPredictorClosable[K](prediction: Double = 0d) extends
      SparseMultiLabelPredictor[K] with Closeable {
      override def apply(v1: SparseFeatures,
        v2: Labels[K],
        v3: LabelIndices,
        v4: SparseLabelDepFeatures): Try[Map[K, Double]] = Try(v2.map(_ -> prediction).toMap)

      def close(): Unit = println("closing")
    }

    val model = MultilabelModel(
      ModelId(),
      sci.IndexedSeq(),
      sci.IndexedSeq[GenAggFunc[Int, Sparse]](),
      sci.IndexedSeq[Label](),
      None,
      Lazy(ConstantPredictorClosable[Label]()),
      None,
      Auditor
    )

    // TODO: How do I test this?
    model.close()
    model.predictorProducer.apply()

    fail()
  }

  @Test def testLabelsOfInterestOmitted(): Unit = {
    // Test labelsAndInfo[A, K] function.
    //
    // When labelsOfInterest = None, labelsAndInfo should return:
    //   LabelsAndInfo[K](
    //     indices = labelsInTrainingSet.indices,
    //     labels = labelsInTrainingSet,
    //     missingLabels = Seq.empty[K],
    //     problems = None
    //   )

    val indices = sci.IndexedSeq[Int](1, 2, 3)
    val labels = sci.IndexedSeq[Label]("label1", "label2", "label3")
    val labelInfo = LabelsAndInfo(
      indices = indices,
      labels = labels,
      missingLabels = Seq[Label](),
      problems = None
    )

    val actual: LabelsAndInfo[String] = labelsAndInfo[Map[String, String], String](
      Map("label1" -> "label1"),
      None,
      Map("label1" -> 1),
      labelInfo
    )

    assertEquals(indices, actual.indices)
    assertEquals(labels, actual.labels)
    assertEquals(Seq.empty[String], actual.missingLabels)
    assertEquals(None, actual.problems)
  }

  @Test def testLabelsOfInterestProvided(): Unit = {
    // Test labelsAndInfo[A, K] function.
    //
    // labelsAndInfo(a, labelsInTrainingSet, labelsOfInterest, labelToInd) ==
    // labelsForPrediction(a, labelsOfInterest.get, labelToInd)

    // TODO: has the signature changed??? still appropriate?
  }
//
  @Test def testReportTooManyMissing(): Unit = {
    // Make sure Subvalue.natural == None
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be None.  Check the errors and missing values.

    val labelInfo = LabelsAndInfo(
      indices = sci.IndexedSeq[Int](),
      labels = sci.IndexedSeq[Label](),
      missingLabels = Seq[Label]("a"),
      problems = None
    )

    val report = reportNoPrediction(
      ModelId(1, "a"),
      labelInfo,
      Auditor
    )

    assertEquals(Vector(NoLabelsError), report.audited.errorMsgs.take(1))
    assertEquals(None, report.audited.value)
  }

  @Test def testReportNoPrediction(): Unit = {
    // Make sure Subvalue.natural == None
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be None.  Check the errors and missing values.

    val labelInfo = LabelsAndInfo(
      indices = sci.IndexedSeq[Int](),
      labels = sci.IndexedSeq[Label](),
      missingLabels = Seq[Label](),
      problems = None
    )

    val report = reportNoPrediction(
      ModelId(1, "a"),
      labelInfo,
      Auditor
    )

    // TODO: check labelInfo values
    assertEquals(None, report.natural)
    assertEquals(None, report.audited.value)
  }

  @Test def testReportNoPredictionMissingLabelsDoNotExist(): Unit = {
    // Make sure Subvalue.natural == None
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be None.  Check the errors and missing values.

    val labelInfo = LabelsAndInfo(
      indices = sci.IndexedSeq[Int](),
      labels = sci.IndexedSeq[Label](),
      missingLabels = missingLabels,
      problems = None
    )

    val report = reportNoPrediction(
      ModelId(1, "a"),
      labelInfo,
      Auditor
    )

    assertEquals(Vector(NoLabelsError) ++ errorMessages, report.audited.errorMsgs)
    assertEquals(None, report.audited.value)
    assertEquals(Set(), report.audited.missingVarNames)
  }


  @Test def testReportPredictorError(): Unit = {
    // Make sure Subvalue.natural == None
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be None.  Check the errors and missing values.

    val labelInfo = LabelsAndInfo(
      indices = sci.IndexedSeq[Int](),
      labels = sci.IndexedSeq[Label](),
      missingLabels = missingLabels,
      problems = None
    )

    val throwable = Try(throw new Exception("error")).failed.get
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    throwable.printStackTrace(pw)
    val stackTrace = sw.toString.split("\n").take(NumLinesToKeepInStackTrace).mkString("\n")

    // This is missing variables for a features
    val missingVariables = Seq("a", "b")
    val missingFeatureMap = scm.Map("x" -> missingVariables)

    val report = reportPredictorError(
      ModelId(-1, "x"),
      labelInfo,
      missingFeatureMap,
      throwable,
      Auditor
    )

    assertEquals(Vector(stackTrace) ++ errorMessages, report.audited.errorMsgs)
    assertEquals(missingVariables.toSet, report.audited.missingVarNames)
    assertEquals(None, report.natural)
    assertEquals(None, report.audited.value)
  }

  @Test def testReportSuccess(): Unit = {
    // Make sure Subvalue.natural == Some(value)
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be Some(value2).
    // 'value' should equal 'value2'.
    // Check the errors and missing values.

    val labelInfo = LabelsAndInfo(
      indices = sci.IndexedSeq[Int](),
      labels = sci.IndexedSeq[Label](),
      missingLabels = missingLabels,
      problems = None
    )
    val predictions = Map("label1" -> 1.0)

    val report = reportSuccess(
      ModelId(0, "ModelId"),
      labelInfo,
      scm.Map("x" -> missingLabels),
      predictions,
      Auditor
    )

    assertEquals(Some(predictions), report.natural)
    assertEquals(Some(predictions), report.audited.value)
    assertEquals(report.natural, report.audited.value)
  }

  @Test def testLabelsForPredictionContainsProblemsWhenLabelsIsEmpty(): Unit = {
    def extractLabelsOutOfExample(example: Map[String, String]): sci.IndexedSeq[String] =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.sorted.toIndexedSeq

    // Example with no problems
    val example: Map[String, String] = Map(
      "feature1" -> "1",
      "feature2" -> "2",
      "feature3" -> "2",
      "label1"   -> "a",
      "label2"   -> "b"
    )
    val allLabels = sci.IndexedSeq("a", "b", "c")
    val labelToInt = allLabels.zipWithIndex.toMap
    val labelsOfInterestExtractor = GenFunc0("empty spec", extractLabelsOutOfExample)
    val labelsAndInfo = labelsForPrediction(example, labelsOfInterestExtractor, labelToInt)
    assertEquals(None, labelsAndInfo.problems)

    // Example with 1 missing label
    val exampleMissingOneLabel = Map("feature1" -> "1", "label1" -> "a")
    val labelsAndInfoMissingOneLabel = labelsForPrediction(
      exampleMissingOneLabel,
      labelsOfInterestExtractor,
      labelToInt)
    assertEquals(None, labelsAndInfoMissingOneLabel.problems)

    // Example with no labels
    val exampleNoLabels = Map("feature1" -> "1", "feature2" -> "2")
    val labelsAndInfoNoLabels = labelsForPrediction(
      exampleNoLabels,
      labelsOfInterestExtractor,
      labelToInt)
    val problemsNoLabelsExpected = Option(GenAggFuncAccessorProblems(Seq(), Seq()))
    assertEquals(problemsNoLabelsExpected, labelsAndInfoNoLabels.problems)

    // missing labels
    def badFunction(example: Map[String, String]) = example.get("feature1")
    val gen1 = GenFunc1("concat _1", (m: Option[String]) => sci.IndexedSeq(s"${m}_1"),
      GeneratedAccessor("extract feature 1", badFunction, None))

    val labelsExtractor =
      (m: Map[String, String]) => m.get("labels") match {
        case ls: sci.IndexedSeq[_] if ls.forall { x: String => x.isInstanceOf[Label] } =>
          Option(ls.asInstanceOf[sci.IndexedSeq[Label]])
        case _ => None
      }

    val f1 =
      GenFunc.f1(GeneratedAccessor("labels", labelsExtractor, None))(
        "def omitted", _ getOrElse sci.IndexedSeq.empty[Label]
      )

    val labelsAndInfoNoLabelsGen1 = labelsForPrediction(
      Map[String, String](),
      f1,
      labelToInt)
    val problemsNoLabelsExpectedGen1 = Option(GenAggFuncAccessorProblems(Seq("labels"), Seq()))
    assertEquals(problemsNoLabelsExpectedGen1, labelsAndInfoNoLabelsGen1.problems)

    // error
    val labelsExtractorError: (Map[String, String]) => Option[sci.IndexedSeq[Label]] =
      (m: Map[String, String]) => m.get("labels") match {
        case ls: sci.IndexedSeq[_] if ls.forall { x: String => x.isInstanceOf[Label] } =>
          Option(ls.asInstanceOf[sci.IndexedSeq[Label]])
        case _ => throw new Exception("labels does not exist")
      }

    val f2 =
      GenFunc.f1(GeneratedAccessor("labels",
        ((m: Map[String, String]) => throw new Exception("errmsg")) : Map[String, String] =>
          Option[sci.IndexedSeq[String]]
        , None))(
        "def omitted", _ getOrElse sci.IndexedSeq.empty[Label]
      )
    val f2Wrapped = EnrichedErrorGenAggFunc(f2)

    new SemanticsUdfException[Any](null, null, null, null, null, null)

    val problemsNoLabelsExpectedGen2 = Option(GenAggFuncAccessorProblems(Seq(), Seq("labels")))
    Try(
    labelsForPrediction(
      Map[String, String](),
      f2Wrapped,
      labelToInt)
    ).failed.get match {
      case ex: SemanticsUdfException[_] =>
        assertEquals(ex.accessorsInErr, problemsNoLabelsExpectedGen2.get.errors)
    }
  }

  @Test def testLabelsForPredictionProvidesLabelsThatCantBePredicted(): Unit = {
    // Test this:
    //    val noPrediction =
    //      if (unsorted.size == labelsShouldPredict.size) Seq.empty
    //      else labelsShouldPredict.filterNot(labelToInd.contains)

    def extractLabelsOutOfExample(example: Map[String, String]) =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.sorted.toIndexedSeq

    val example: Map[String, String] = Map(
      "feature1" -> "1",
      "feature2" -> "2",
      "feature3" -> "2",
      "label1"   -> "a",
      "label2"   -> "b"
    )
    val allLabels = sci.IndexedSeq("a", "b", "c")
    val labelToInt = allLabels.zipWithIndex.toMap
    val labelsOfInterestExtractor = GenFunc0("empty spec", extractLabelsOutOfExample)
    val labelsAndInfo = labelsForPrediction(example, labelsOfInterestExtractor, labelToInt)
    val missingLabels = labelsAndInfo.missingLabels
    assertEquals(Seq(), missingLabels)

    // Extra label not in the list
    val example2 = Map("label4" -> "d")
    val labelsAndInfo2 = labelsForPrediction(example2, labelsOfInterestExtractor, labelToInt)
    val missingLabels2 = labelsAndInfo2.missingLabels
    assertEquals(Seq("d"), missingLabels2)

    // No labels
    val example3 = Map("feature2" -> "5")
    val labelsAndInfo3 = labelsForPrediction(example3, labelsOfInterestExtractor, labelToInt)
    val missingLabels3 = labelsAndInfo3.missingLabels
    assertEquals(Seq(), missingLabels3)
  }

  @Test def testLabelsForPredictionReturnsLabelsSortedByIndex(): Unit = {
    // Test this:
    //    val (ind, lab) = unsorted.sortBy{ case (i, _) => i }.unzip

    def extractLabelsOutOfExample(example: Map[String, String]) =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.sorted.toIndexedSeq

    val example: Map[String, String] = Map(
      "feature1" -> "1",
      "feature2" -> "2",
      "feature3" -> "2",
      "label1"   -> "a",
      "label2"   -> "b",
      "label3"   -> "l23",
      "label4"   -> "100",
      "label5"   -> "235",
      "label6"   -> "c",
      "label7"   -> "1",
      "label8"   -> "l1"
    )

    val allLabels = sci.IndexedSeq("a", "b", "c", "235", "1", "l1", "l23", "100")
    val labelToInt = allLabels.zipWithIndex.toMap
    val labelsOfInterestExtractor = GenFunc0("empty spec", extractLabelsOutOfExample)
    val labelsAndInfo = labelsForPrediction(example, labelsOfInterestExtractor, labelToInt)

    assertEquals(allLabels, labelsAndInfo.labels)
    assertEquals(allLabels.indices, labelsAndInfo.indices)
  }

  @Test def testSubvalueReportsNoPredictionWhenNoLabelsAreProvided(): Unit = {
    // Test this:
    //    if (li.labels.isEmpty)
    //      reportNoPrediction(modelId, li, auditor)
    def extractLabelsOutOfExample(example: Map[String, String]) =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.sorted.toIndexedSeq

    val example = Map("" -> "")
    val allLabels = sci.IndexedSeq("a", "b", "c")
    val labelToInd = allLabels.zipWithIndex.toMap
    val labelsOfInterestExtractor = GenFunc0("empty spec", extractLabelsOutOfExample)
    val defaultLabelInfo = LabelsAndInfo(allLabels.indices, allLabels, Seq.empty, None)
    val li = labelsAndInfo(example, Option(labelsOfInterestExtractor), labelToInd, defaultLabelInfo)
    val report = reportNoPrediction(ModelId(), li, aud)
    assertEquals(Vector(NoLabelsError), report.audited.errorMsgs)
  }

  @Test def testSubvalueReportsTooManyMissingWhenThereAreTooManyMissingFeatures(): Unit = {
    // When the amount of missing data exceeds the threshold, reportTooManyMissing should be
    // called and its value should be returned.  Instantiate a MultilabelModel and
    // call apply with some missing data required by the features.

    // TODO: this is a terrible setup
    val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
      GenFunc0("""Iterable(("", 1d))""",
        (x: Map[String, String]) => if (x.keys.toVector.contains("a")) {
          Iterable((x.keys.head, 1d))
      } else {
          Iterable()
      })

    val featureFunctions = Vector.fill(4)(EmptyIndicatorFn)

    val modelWithThreshold = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq("a", "b", "c", "d"),
      featureFunctions    = featureFunctions,
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2", "label3", "label4"),
      labelsOfInterest    = None,
      predictorProducer   = Lazy(ConstantPredictor[Label]()),
      numMissingThreshold = Option(0),
      auditor             = Auditor
    )

    val result = modelWithThreshold(Map())
    assertEquals(TooManyMissingError, result.errorMsgs.head)
  }

  @Test def testExceptionsThrownByPredictorAreHandledGracefully(): Unit = {
    // Create a predictorProducer that throws.  Check that the model still returns a value
    // and that the error message is incorporated appropriately.

    val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
      GenFunc0("""Iterable(("", 1d))""",
        (x: Map[String, String]) => if (x.keys.toVector.contains("a")) {
          Iterable((x.keys.head, 1d))
        } else {
          Iterable()
        })

    val featureFunctions = Vector.fill(4)(EmptyIndicatorFn)

    case class PredictorThatThrows[K](prediction: Double = 0d) extends
      SparseMultiLabelPredictor[K] {
      override def apply(v1: SparseFeatures,
        v2: Labels[K],
        v3: LabelIndices,
        v4: SparseLabelDepFeatures): Try[Map[K, Double]] = Try(throw new Exception("error"))
    }
    val modelWithThrowingPredictorProducer = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq("a", "b", "c", "d"),
      featureFunctions    = featureFunctions,
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2", "label3", "label4"),
      labelsOfInterest    = None,
      predictorProducer   = Lazy(PredictorThatThrows[Label]()),
      numMissingThreshold = None,
      auditor             = Auditor
    )
    val result = modelWithThrowingPredictorProducer(Map())
    assertEquals(None, result.value)
    assert(result.errorMsgs.head.contains("java.lang.Exception: error"))
  }

  @Test def testSubvalueSuccess(): Unit = {
    // Test the happy path by calling model.apply. Check the value, missing data, and error
    // messages.

    val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
      GenFunc0("""Iterable(("", 1d))""", (_: Any) => Iterable(("", 1d)))

    val featureFunctions = Vector.fill(4)(EmptyIndicatorFn)

    def extractLabelsOutOfExample(example: Map[String, String]) =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.sorted.toIndexedSeq

    val labelsOfInterestExtractor = GenFunc0("empty spec", extractLabelsOutOfExample)

    val modelSuccess = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq("a", "b", "c", "d"),
      featureFunctions    = featureFunctions,
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2", "label3", "label4"),
      labelsOfInterest    = Option(labelsOfInterestExtractor),
      predictorProducer   = Lazy(ConstantPredictor[Label]()),
      numMissingThreshold = None,
      auditor             = Auditor
    )
    val result = modelSuccess(Map("a" -> "b", "label1" -> "label1", "label2" -> "label2"))
    assertEquals(Vector(), result.errorMsgs)
    assertEquals(Set(), result.missingVarNames)
    assertEquals(Option(Map("label1" -> 0.0, "label2" -> 0.0)), result.value)
  }

  @Test def testExceptionsThrownInFeatureFunctionsAreNotCaught(): Unit = {
    // NOTE: This is by design.

    // TODO: this actually throws
    val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
      GenFunc0("""Iterable(("", 1d))""", (x: Map[String, String]) => Iterable((x("hello"), 1d)))

    val featureFunctions = Vector.fill(4)(EmptyIndicatorFn)

    val modelSuccess = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq("a", "b", "c", "d"),
      featureFunctions    = featureFunctions,
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2", "label3", "label4"),
      labelsOfInterest    = None,
      predictorProducer   = Lazy(ConstantPredictor[Label]()),
      numMissingThreshold = None,
      auditor             = Auditor
    )
    val result = modelSuccess(Map("a" -> "b", "label1" -> "label1", "label2" -> "label2"))

    println(result.value)
  }
}

object MultilabelModelTest {
  // TODO: Use this label type and Auditor.

  private type Label = String
  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]]()

  case class ConstantPredictor[K](prediction: Double = 0d) extends SparseMultiLabelPredictor[K] {
    override def apply(v1: SparseFeatures,
      v2: Labels[K],
      v3: LabelIndices,
      v4: SparseLabelDepFeatures): Try[Map[K, Double]] = Try(v2.map(_ -> prediction).toMap)
  }

  case class Lazy[A](value: A) extends (() => A) {
    override def apply(): A = value
  }

  val missingLabels: Seq[Label] = Seq("a", "b")

  val modelNoFeatures = MultilabelModel(
    ModelId(),
    sci.IndexedSeq(),
    sci.IndexedSeq[GenAggFunc[Int, Sparse]](),
    sci.IndexedSeq[Label](),
    None,
    Lazy(ConstantPredictor[Label]()),
    None,
    Auditor
  )

  val aud: RootedTreeAuditor[Any, Map[Label, Double]] = RootedTreeAuditor[Any, Map[Label, Double]]()
  // private val failure = aud.failure()

  val baseErrorMessage: Seq[String] = Stream.continually("Label not in training labels: ")
  val errorMessages: Seq[String] = baseErrorMessage.zip(missingLabels).map {
    case(msg, label) => s"$msg$label"
  }

// TODO: Access information returned in audited value by using the following functions:
  //    val aud: RootedTree[Any, Map[Label, Double]] = ???
  //    aud.modelId           // : ModelIdentity
  //    aud.value             // : Option[Map[Label, Double]]  // Should be missing on failure.
  //    aud.missingVarNames   // : Set[String]
  //    aud.errorMsgs         // : Seq[String]
  //    aud.prob              // : Option[Float]  (Shouldn't need this)
}
