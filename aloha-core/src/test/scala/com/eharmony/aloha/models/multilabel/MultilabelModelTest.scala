package com.eharmony.aloha.models.multilabel

import java.io.{Closeable, PrintWriter, StringWriter}
import java.util.concurrent.atomic.AtomicBoolean

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.tree.RootedTreeAuditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.multilabel.MultilabelModel.{reportNoPrediction, LabelsAndInfo, NumLinesToKeepInStackTrace}
import com.eharmony.aloha.semantics.SemanticsUdfException
import com.eharmony.aloha.semantics.func._
import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Random, Success, Try}

/**
  * Created by ryan.deak and amirziai on 9/1/17.
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
    case class PredictorClosable[K](prediction: Double = 0d)
      extends SparseMultiLabelPredictor[K] with Closeable {
      def apply(
        v1: SparseFeatures,
        v2: Labels[K],
        v3: LabelIndices,
        v4: SparseLabelDepFeatures) = Try(Map())
      private[this] val closed = new AtomicBoolean(false)
      override def close(): Unit = closed.set(true)
      def isClosed: Boolean = closed.get()
    }

    val predictor = PredictorClosable[Label]()
    val model = modelNoFeatures.copy(predictorProducer = Lazy(predictor))

    model.close()
    assertTrue(predictor.isClosed)
  }

  @Test def testLabelsOfInterestOmitted(): Unit = {
    // When labelsOfInterest = None, labelsAndInfo should return:
    //   LabelsAndInfo[K](
    //     indices = labelsInTrainingSet.indices,
    //     labels = labelsInTrainingSet,
    //     missingLabels = Seq.empty[K],
    //     problems = None
    //   )

    val actual: LabelsAndInfo[Label] = labelsAndInfo(
      a                = (),
      labelsOfInterest = None,
      labelToInd       = Map.empty,
      defaultLabelInfo = labelsAndInfoEmpty
    )
    assertEquals(labelsAndInfoEmpty, actual)
  }

  @Test def testLabelsOfInterestProvided(): Unit = {
    val a = ()
    val labelsOfInterest =
      Option(GenFunc0[Unit,sci.IndexedSeq[Label]]("", _ => labelsInTrainingSet))
    val actual: LabelsAndInfo[Label] = labelsAndInfo(
      a = a,
      labelsOfInterest = labelsOfInterest,
      Map.empty,
      labelsAndInfoEmpty
    )
    val expected = labelsForPrediction(a, labelsOfInterest.get, Map.empty[Label, Int])
    assertEquals(expected, actual)
  }

  @Test def testReportTooManyMissing(): Unit = {
    // labelsAndInfoEmpty.copy(missingLabels = Seq[Label]("a")),

    val labelInfo = LabelsAndInfo(
      indices = sci.IndexedSeq[Int](),
      labels = sci.IndexedSeq[Label](),
      missingLabels = Seq[Label]("a"),
      problems = None
    )

    // TODO: reportTooManyMissing
    val report = reportNoPrediction(
      ModelId(1, "a"),
      labelInfo,
      Auditor
    )

    assertEquals(Vector(NoLabelsError), report.audited.errorMsgs.take(1))
    assertEquals(None, report.audited.value)
    assertEquals(None, report.natural)

    fail()
  }

  @Test def testReportNoPrediction(): Unit = {
    val report = reportNoPredictionEmpty
    assertEquals(Vector(NoLabelsError), report.audited.errorMsgs)
    assertEquals(None, report.natural)
    assertEquals(None, report.audited.value)
  }

  @Test def testReportNoPredictionMissingLabelsDoNotExist(): Unit = {
    // The missing labels are reported in the error message

    val report = reportNoPredictionPartial(labelsAndInfoMissingLabels)
    assertEquals(Vector(NoLabelsError) ++ errorMessages, report.audited.errorMsgs)
    assertEquals(None, reportNoPredictionEmpty.audited.value)
    assertEquals(Set(), report.audited.missingVarNames)
  }

  @Test def testReportPredictorError(): Unit = {
    val (throwable, stackTrace) = getThrowable("error")
    val missingVariables = Seq("a", "b")
    val report = reportPredictorError(
      ModelId(),
      labelsAndInfoEmpty.copy(missingLabels = missingLabels),
      scm.Map("" -> missingVariables),
      throwable,
      Auditor
    )
    assertEquals(Vector(stackTrace) ++ errorMessages, report.audited.errorMsgs)
    assertEquals(missingVariables.toSet, report.audited.missingVarNames)
    assertEquals(None, report.natural)
    assertEquals(None, report.audited.value)
  }

  @Test def testReportSuccess(): Unit = {
    val predictions = Map("label" -> 1.0)
    val report   = reportSuccess(
      modelId    = ModelId(),
      labelInfo  = labelsAndInfoEmpty.copy(missingLabels = missingLabels),
      missing    = scm.Map("" -> missingLabels),
      prediction = predictions,
      auditor    = Auditor
    )
    assertEquals(Some(predictions), report.natural)
    assertEquals(Some(predictions), report.audited.value)
    assertEquals(report.natural, report.audited.value)
  }

  @Test def testLabelsForPredictionContainsProblemsWhenLabelsIsEmpty(): Unit = {
    def extractLabelsOutOfExample(example: Map[String, String]): sci.IndexedSeq[String] =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.sorted.toIndexedSeq

    // TODO: break this up

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
    // No problems are actually found
    // But the important thing is that problemsNoLabelsExpected is a Some and not a None
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

    val featureWithMissingValue = "feature not present"
    val f1 =
      GenFunc.f1(GeneratedAccessor(featureWithMissingValue, labelsExtractor, None))(
        "def omitted", _ getOrElse sci.IndexedSeq.empty[Label]
      )

    val labelsAndInfoNoLabelsGen1 = labelsForPrediction(
      Map[String, String](),
      f1,
      labelToInt)
    val problemsNoLabelsExpectedGen1 = Option(GenAggFuncAccessorProblems(Seq(featureWithMissingValue), Seq()))
    assertEquals(problemsNoLabelsExpectedGen1, labelsAndInfoNoLabelsGen1.problems)

    // error
    val featureWithError = "feature has error"
    val f2 =
      GenFunc.f1(GeneratedAccessor(featureWithError,
        ( _ => throw new Exception("errmsg")) : Map[String, String] => Option[sci.IndexedSeq[String]], None))(
        "def omitted", _ getOrElse sci.IndexedSeq.empty[Label]
      )
    val f2Wrapped = EnrichedErrorGenAggFunc(f2)

    val problemsNoLabelsExpectedGen2 = Option(GenAggFuncAccessorProblems(Seq(), Seq(featureWithError)))
    Try(
    labelsForPrediction(
      Map[String, String](),
      f2Wrapped,
      labelToInt)
    ).failed.get match {
      case SemanticsUdfException(_, _, _, accessorsInErr, _, _) => assertEquals(accessorsInErr,
        problemsNoLabelsExpectedGen2.get.errors)
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

    // TODO: add these as a different test at the model.apply(.) level
    // assertEquals(None, modelNew(Vector("1")))
  }

  @Test def testLabelsForPredictionReturnsLabelsSortedByIndex(): Unit = {
    // Test this:
    //    val (ind, lab) = unsorted.sortBy{ case (i, _) => i }.unzip

    def extractLabelsOutOfExample(example: Map[String, String]) =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.toIndexedSeq //  sorted.toIndexedSeq

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

    val allLabels = extractLabelsOutOfExample(example).sorted
    val labelToInt: Map[String, Int] = allLabels.zipWithIndex.toMap

    val rng = new Random(seed=0)
    (1 to 10).foreach { _ =>
      val ex = rng.shuffle(example.toVector).take(rng.nextInt(example.size)).toMap
      val labelsOfInterestExtractor = GenFunc0("empty spec", extractLabelsOutOfExample)
      val labelsAndInfo = labelsForPrediction(ex, labelsOfInterestExtractor, labelToInt)
      assertEquals(labelsAndInfo.indices.sorted, labelsAndInfo.indices)
    }
  }

  @Test def testSubvalueReportsNoPredictionWhenNoLabelsAreProvided(): Unit = {
    // Test this:
    //    if (li.labels.isEmpty)
    //      reportNoPrediction(modelId, li, auditor)

    assertEquals(None, modelNew.subvalue(Vector.empty).natural)
  }

  @Test def testSubvalueReportsTooManyMissingWhenThereAreTooManyMissingFeatures(): Unit = {
    // When the amount of missing data exceeds the threshold, reportTooManyMissing should be
    // called and its value should be returned.  Instantiate a MultilabelModel and
    // call apply with some missing data required by the features.

    val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
      GenFunc0("", _ => Iterable())

    val featureFunctions = Vector(EmptyIndicatorFn)

    val modelWithThreshold = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq("a"),
      featureFunctions    = featureFunctions,
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2"),
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

    case object PredictorThatThrows extends
      SparseMultiLabelPredictor[Label] {
      override def apply(v1: SparseFeatures,
        v2: Labels[Label],
        v3: LabelIndices,
        v4: SparseLabelDepFeatures): Try[Map[Label, Double]] = Try(throw new Exception("error"))
    }

    val modelWithThrowingPredictorProducer = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq.empty,
      featureFunctions    = sci.IndexedSeq.empty,
      labelsInTrainingSet = sci.IndexedSeq(""),  // we need at least 1 label to get the error
      labelsOfInterest    = None,
      predictorProducer   = Lazy(PredictorThatThrows),
      numMissingThreshold = None,
      auditor             = Auditor
    )

    val result = modelWithThrowingPredictorProducer(Map())
    assertEquals(None, result.value)
    assertEquals("java.lang.Exception: error", result.errorMsgs.head.split("\n").head)
  }

  @Test def testSubvalueSuccess(): Unit = {
    // Test the happy path by calling model.apply. Check the value, missing data, and error
    // messages.

    def extractLabelsOutOfExample(example: Map[String, String]) =
      example.filterKeys(_.startsWith("label")).toSeq.unzip._2.sorted.toIndexedSeq

    val labelsOfInterestExtractor = GenFunc0("empty spec", extractLabelsOutOfExample)

    val scoreToReturn = 5d

    val modelSuccess = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq.empty,
      featureFunctions    = sci.IndexedSeq.empty,
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2", "label3", "label4"),
      labelsOfInterest    = Option(labelsOfInterestExtractor),
      predictorProducer   = Lazy(ConstantPredictor[Label](scoreToReturn)),
      numMissingThreshold = None,
      auditor             = Auditor
    )
    val result = modelSuccess(Map("a" -> "b", "label1" -> "label1", "label2" -> "label2"))
    assertEquals(Vector(), result.errorMsgs)
    assertEquals(Set(), result.missingVarNames)
    assertEquals(Option(Map("label1" -> scoreToReturn, "label2" -> scoreToReturn)), result.value)
  }

  @Test def testExceptionsThrownInFeatureFunctionsAreNotCaught(): Unit = {
    // NOTE: This is by design.

    val exception = new Exception("error")
    val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
      GenFunc0("", _ => throw exception)

    val featureFunctions = Vector(EmptyIndicatorFn)

    val modelSuccess = MultilabelModel(
      modelId             = ModelId(1, "model1"),
      featureNames        = sci.IndexedSeq("throwing feature"),
      featureFunctions    = featureFunctions,
      labelsInTrainingSet = sci.IndexedSeq[Label](""),
      labelsOfInterest    = None,
      predictorProducer   = Lazy(ConstantPredictor[Label]()),
      numMissingThreshold = None,
      auditor             = Auditor
    )


    val result = Try(modelSuccess(Map()))
    result match {
      case Success(_) => fail()
      case Failure(ex) => assertEquals(exception, ex)
    }
  }
}

object MultilabelModelTest {
  private type Label = String
  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]]()

  case class ConstantPredictor[K](prediction: Double = 0d) extends SparseMultiLabelPredictor[K] {
    override def apply(featuresUnused: SparseFeatures,
      labels: Labels[K],
      indicesUnused: LabelIndices,
      ldfUnused: SparseLabelDepFeatures): Try[Map[K, Double]] =
        Try(labels.map(_ -> prediction).toMap)
  }

  case class Lazy[A](value: A) extends (() => A) {
    override def apply(): A = value
  }

  val labelsInTrainingSet = sci.IndexedSeq[Label]("a", "b", "c")

  // Models
  val modelNoFeatures = MultilabelModel(
    modelId             = ModelId(),
    featureNames        = sci.IndexedSeq(),
    featureFunctions    = sci.IndexedSeq[GenAggFunc[Int, Sparse]](),
    labelsInTrainingSet = sci.IndexedSeq[Label](),
    labelsOfInterest    = None,
    predictorProducer   = Lazy(ConstantPredictor[Label]()),
    numMissingThreshold = None,
    auditor             = Auditor
  )

  val modelNew = modelNoFeatures.copy(
    featureFunctions    = sci.IndexedSeq[GenAggFunc[Vector[String], Sparse]](),
    labelsInTrainingSet = labelsInTrainingSet,
    labelsOfInterest    = Some(GenFunc0("", (a: Vector[String]) => a))
  )

  // LabelsAndInfo
  val labelsAndInfoEmpty = LabelsAndInfo(
    indices = sci.IndexedSeq.empty,
    labels = sci.IndexedSeq.empty,
    missingLabels = Seq[Label](),
    problems = None
  )
  val labelsAndInfoMissingLabels = labelsAndInfoEmpty.copy(missingLabels = missingLabels)

  // Reports
  val reportNoPredictionPartial = reportNoPrediction(
    modelId   = ModelId(),
    _: LabelsAndInfo[Label],
    auditor   = Auditor
  )
  val reportNoPredictionEmpty = reportNoPredictionPartial(labelsAndInfoEmpty)

  val auditor: RootedTreeAuditor[Any, Map[Label, Double]] =
    RootedTreeAuditor[Any, Map[Label, Double]]()

  val baseErrorMessage: Seq[String] = Stream.continually("Label not in training labels: ")
  val missingLabels: Seq[Label] = Seq("a", "b")
  val errorMessages: Seq[String] = baseErrorMessage.zip(missingLabels).map {
    case(msg, label) => s"$msg$label"
  }

  def getThrowable(errorMessage: String): (Throwable, String) = {
    val throwable = Try(throw new Exception(errorMessage)).failed.get
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    throwable.printStackTrace(pw)
    val stackTrace = sw.toString.split("\n").take(NumLinesToKeepInStackTrace).mkString("\n")
    (throwable, stackTrace)
  }
}
