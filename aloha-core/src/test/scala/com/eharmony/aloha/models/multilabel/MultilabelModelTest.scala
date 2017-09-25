package com.eharmony.aloha.models.multilabel

import java.io.{Closeable, PrintWriter, StringWriter}
import java.util.concurrent.atomic.AtomicBoolean

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.tree.{RootedTree, RootedTreeAuditor, Tree}
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.Subvalue
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
    val modelRoundTrip = serializeDeserializeRoundTrip(ModelNoFeatures)
    assertEquals(ModelNoFeatures, modelRoundTrip)
  }

  @Test def testModelCloseClosesPredictor(): Unit = {
    class PredictorClosable[K] extends SparseMultiLabelPredictor[K] with Closeable {
      def apply(
        v1: SparseFeatures,
        v2: Labels[K],
        v3: LabelIndices,
        v4: SparseLabelDepFeatures) = Try(Map())
      private[this] val closed = new AtomicBoolean(false)
      override def close(): Unit = closed.set(true)
      def isClosed: Boolean = closed.get()
    }

    val predictor = new PredictorClosable[Label]
    val model = ModelNoFeatures.copy(predictorProducer = Lazy(predictor))

    model.close()
    assertTrue(predictor.isClosed)
  }

  @Test def testLabelsOfInterestOmitted(): Unit = {
    val actual: LabelsAndInfo[Label] = labelsAndInfo(
      a                = (),
      labelsOfInterest = None,
      labelToInd       = Map.empty,
      defaultLabelInfo = LabelsAndInfoEmpty
    )
    assertEquals(LabelsAndInfoEmpty, actual)
  }

  @Test def testLabelsOfInterestProvided(): Unit = {
    val a = ()
    val labelsOfInterest = GenFunc0[Unit,sci.IndexedSeq[Label]]("", _ => LabelsInTrainingSet)
    val actual: LabelsAndInfo[Label] = labelsAndInfo(
      a = a,
      labelsOfInterest = Option(labelsOfInterest),
      Map.empty,
      LabelsAndInfoEmpty
    )
    val expected = labelsForPrediction(a, labelsOfInterest, Map.empty[Label, Int])
    assertEquals(expected, actual)
  }

  @Test def testReportTooManyMissing(): Unit = {
    val report  = reportTooManyMissing(
      modelId   = ModelId(),
      labelInfo = LabelsAndInfoEmpty,
      missing   = scm.Map("" -> LabelsNotInTrainingSet),
      auditor   = Auditor
    )

    assertEquals(Vector(TooManyMissingError), report.audited.errorMsgs.take(1))
    assertEquals(None, report.audited.value)
    assertEquals(None, report.natural)
  }

  @Test def testReportNoPrediction(): Unit = {
    val report = ReportNoPredictionEmpty
    assertEquals(Vector(NoLabelsError), report.audited.errorMsgs)
    assertEquals(None, report.natural)
    assertEquals(None, report.audited.value)
  }

  @Test def testReportNoPredictionMissingLabelsDoNotExist(): Unit = {
    // The missing labels are reported in the error message

    val report = ReportNoPredictionPartial(LabelsAndInfoMissingLabels)
    assertEquals(Vector(NoLabelsError) ++ ErrorMessages, report.audited.errorMsgs)
    assertEquals(None, ReportNoPredictionEmpty.audited.value)
    assertEquals(Set(), report.audited.missingVarNames)
  }

  @Test def testReportPredictorError(): Unit = {
    val (throwable, stackTrace) = getThrowable("error")
    val missingVariables = Seq("a", "b")
    val report = reportPredictorError(
      ModelId(),
      LabelsAndInfoEmpty.copy(labelsNotInTrainingSet = LabelsNotInTrainingSet),
      scm.Map("" -> missingVariables),
      throwable,
      Auditor
    )
    assertEquals(Vector(stackTrace) ++ ErrorMessages, report.audited.errorMsgs)
    assertEquals(missingVariables.toSet, report.audited.missingVarNames)
    assertEquals(None, report.natural)
    assertEquals(None, report.audited.value)
  }

  @Test def testReportSuccess(): Unit = {
    val predictions = Map("label" -> 1.0)
    val report   = reportSuccess(
      modelId    = ModelId(),
      labelInfo  = LabelsAndInfoEmpty.copy(labelsNotInTrainingSet = LabelsNotInTrainingSet),
      missing    = scm.Map("" -> LabelsNotInTrainingSet),
      prediction = predictions,
      auditor    = Auditor
    )
    assertEquals(Some(predictions), report.natural)
    assertEquals(Some(predictions), report.audited.value)
    assertEquals(report.natural, report.audited.value)
  }

  @Test def testLabelsForPredictionContainsProblemsWhenNoLabelProvided(): Unit = {
    val labelsAndInfoNoLabels = labelsForPrediction(
      example          = Map[String, String](),  // no label provided
      labelsOfInterest = LabelsOfInterestExtractor,
      labelToInd       = LabelsInTrainingSetToIndex)

    // In this scenario no problems are found but an
    // Option(GenAggFuncAccessorProblems) is returned instead of None
    val problemsNoLabelsExpected = Option(GenAggFuncAccessorProblems(Seq(), Seq()))
    assertEquals(problemsNoLabelsExpected, labelsAndInfoNoLabels.problems)
  }

  @Test def testLabelsForPredictionContainsProblemsWhenLabelsIsNotPresent(): Unit = {
    val labelsExtractor =
      (label: Map[Label, Label]) => label.get("labels").map(sci.IndexedSeq[Label](_))
    val descriptor = "label is missing"
    val labelsOfInterest =
      GenFunc.f1(GeneratedAccessor(descriptor, labelsExtractor, None))(
        "", _ getOrElse sci.IndexedSeq.empty[Label]
      )
    val labelsAndInfoNoLabelsGen1 = labelsForPrediction(
      example          = Map[Label, Label](),
      labelsOfInterest = labelsOfInterest,
      labelToInd       = LabelsInTrainingSetToIndex)
    val problemsNoLabelsExpectedGen1 =
      Option(GenAggFuncAccessorProblems(Seq(descriptor), Seq()))
    assertEquals(problemsNoLabelsExpectedGen1, labelsAndInfoNoLabelsGen1.problems)
  }

  @Test def testLabelsForPredictionContainsProblemsWhenLabelAccessorThrows(): Unit = {
    val descriptor = "label accessor that throws"
    val labelsOfInterest =
      GenFunc.f1(GeneratedAccessor(descriptor,
        ( _ => throw new Exception()) : Map[String, String] => Option[sci.IndexedSeq[String]], None))(
        "", _ getOrElse sci.IndexedSeq.empty[Label]
      )
    val labelsOfInterestWrapped = EnrichedErrorGenAggFunc(labelsOfInterest)

    val problemsNoLabelsExpected = Option(GenAggFuncAccessorProblems(Seq(), Seq(descriptor)))
    Try {
      labelsForPrediction(Map.empty[String, String], labelsOfInterestWrapped,
        LabelsInTrainingSetToIndex)
    } match {
      // Failure is scala.util.Failure
      case Failure(SemanticsUdfException(_, _, _, accessorsInErr, _, _)) =>
        assertEquals(accessorsInErr, problemsNoLabelsExpected.get.errors)
      case _ => fail()
    }
  }

  @Test def testLabelsForPredictionProvidesLabelsThatCantBePredicted(): Unit = {
    // This test would be better done with a property-based testing framework such as ScalaCheck
    val labelNotInTrainingSet = "d"
    val labelsAndInfo = labelsForPrediction(Map("label" -> labelNotInTrainingSet),
      LabelsOfInterestExtractor, LabelsInTrainingSetToIndex)
    assertEquals(Seq(labelNotInTrainingSet), labelsAndInfo.labelsNotInTrainingSet)
  }

  @Test def testLabelsForPredictionReturnsLabelsSortedByIndex(): Unit = {
    val example: Map[String, String] = Map(
      "feature1" -> "1",
      "label1"   -> "a",
      "label3"   -> "l23",
      "label4"   -> "100",
      "label6"   -> "c",
      "label8"   -> "l1"
    )

    val allLabels = extractLabelsOutOfExample(example).sorted
    val labelToInt = allLabels.zipWithIndex.toMap

    val random = new Random(seed=0)
    (1 to 10).foreach { _ =>
      val ex = random.shuffle(example.toVector).take(random.nextInt(example.size)).toMap
      val labelsOfInterestExtractor = GenFunc0("", extractLabelsOutOfExample)
      val labelsAndInfo = labelsForPrediction(ex, labelsOfInterestExtractor, labelToInt)
      assertEquals(labelsAndInfo.indices.sorted, labelsAndInfo.indices)
    }
  }

  @Test def testSubvalueReportsNoPredictionWhenNoLabelsAreProvided(): Unit = {
    assertEquals(None, ModelWithFeatureFunctions.subvalue(Vector.empty).natural)
  }

  @Test def testSubvalueReportsTooManyMissingWhenThereAreTooManyMissingFeatures(): Unit = {
    // When the amount of missing data exceeds the threshold, reportTooManyMissing should be
    // called and its value should be returned.

    val modelWithMissingThreshold  = ModelWithFeatureFunctions.copy(
      featureNames = sci.IndexedSeq("feature1"),
      featureFunctions = FeatureFunctions,
      labelsInTrainingSet = LabelsInTrainingSet,
      labelsOfInterest = None,
      numMissingThreshold = Option(0)
    )

    val result = modelWithMissingThreshold(Map.empty)
    assertEquals(None, result.value)
    assertEquals(TooManyMissingError, result.errorMsgs.head)
  }

  @Test def testExceptionsThrownByPredictorAreHandledGracefully(): Unit = {
    case object PredictorThatThrows extends
      SparseMultiLabelPredictor[Label] {
      override def apply(v1: SparseFeatures,
        v2: Labels[Label],
        v3: LabelIndices,
        v4: SparseLabelDepFeatures): Try[Map[Label, Double]] = Try(throw new Exception("error"))
    }

    val modelWithThrowingPredictorProducer = ModelWithFeatureFunctions.copy(
      predictorProducer = Lazy(PredictorThatThrows),
      labelsOfInterest  = None
    )

    val result = modelWithThrowingPredictorProducer(Vector.empty)
    assertEquals(None, result.value)
    assertEquals("java.lang.Exception: error", result.errorMsgs.head.split("\n").head)
  }

  @Test def testSubvalueSuccess(): Unit = {
    val scoreToReturn = 5d
    val modelSuccess = ModelNoFeatures.copy(
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2", "label3", "label4"),
      labelsOfInterest    = Option(LabelsOfInterestExtractor),
      predictorProducer   = Lazy(ConstantPredictor[Label](scoreToReturn)),
      featureFunctions    = sci.IndexedSeq.empty
    )

    val result = modelSuccess(Map("a" -> "b", "label1" -> "label1", "label2" -> "label2"))
    assertEquals(Vector(), result.errorMsgs)
    assertEquals(Set(), result.missingVarNames)
    assertEquals(Option(Map("label1" -> scoreToReturn, "label2" -> scoreToReturn)), result.value)
  }

  @Test def testExceptionsThrownInFeatureFunctionsAreNotCaught(): Unit = {
    // This is by design.

    val exception = new Exception("error")
    val featureFunctionThatThrows: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
      GenFunc0("", _ => throw exception)

    val modelWithFeatureFunctionThatThrows = ModelNoFeatures.copy(
      featureNames        = sci.IndexedSeq("throwing feature"),
      featureFunctions    = Vector(featureFunctionThatThrows),
      labelsInTrainingSet = sci.IndexedSeq[Label](""),
      labelsOfInterest    = None
    )

    val result = Try(modelWithFeatureFunctionThatThrows(Map()))
    result match {
      case Success(_) => fail()
      case Failure(ex) => assertEquals(exception, ex)
    }
  }
}

object MultilabelModelTest {
  // Types
  private type Label = String
  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]]()

  private case class ConstantPredictor[K](prediction: Double = 0d) extends
  SparseMultiLabelPredictor[K] {
    override def apply(featuresUnused: SparseFeatures,
      labels: Labels[K],
      indicesUnused: LabelIndices,
      ldfUnused: SparseLabelDepFeatures): Try[Map[K, Double]] =
        Try(labels.map(_ -> prediction).toMap)
  }

  private case class Lazy[A](value: A) extends (() => A) {
    override def apply(): A = value
  }

  // Common input
  private val LabelsInTrainingSet: sci.IndexedSeq[Label] = sci.IndexedSeq[Label]("a", "b", "c")
  private val LabelsInTrainingSetToIndex: Map[Label, Int] = LabelsInTrainingSet.zipWithIndex.toMap
  private val LabelsNotInTrainingSet: Seq[Label] = Seq("a", "b")
  private val BaseErrorMessage: Seq[String] = Stream.continually("Label not in training labels: ")
  private val ErrorMessages: Seq[String] = BaseErrorMessage.zip(LabelsNotInTrainingSet).map {
    case(msg, label) => s"$msg$label"
  }

  // Feature functions
  private val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
    GenFunc0("", _ => Iterable())
  private val FeatureFunctions = Vector(EmptyIndicatorFn)

  // Models
  private val ModelNoFeatures = MultilabelModel(
    modelId             = ModelId(),
    featureNames        = sci.IndexedSeq(),
    featureFunctions    = sci.IndexedSeq[GenAggFunc[Int, Sparse]](),
    labelsInTrainingSet = sci.IndexedSeq[Label](),
    labelsOfInterest    = None,
    predictorProducer   = Lazy(ConstantPredictor[Label]()),
    numMissingThreshold = None,
    auditor             = Auditor
  )

  private val ModelWithFeatureFunctions:
    MultilabelModel[Tree[Any], String, Vector[String], RootedTree[Any, Map[Label, Double]]] =
    ModelNoFeatures.copy(
    featureFunctions    = sci.IndexedSeq[GenAggFunc[Vector[String], Sparse]](),
    labelsInTrainingSet = LabelsInTrainingSet,
    labelsOfInterest    = Some(GenFunc0("", (a: Vector[String]) => a))
  )

  // LabelsAndInfo
  private val LabelsAndInfoEmpty = LabelsAndInfo(
    indices = sci.IndexedSeq.empty,
    labels = sci.IndexedSeq.empty,
    labelsNotInTrainingSet = Seq.empty[Label],
    problems = None
  )
  private val LabelsAndInfoMissingLabels: LabelsAndInfo[Label] =
    LabelsAndInfoEmpty.copy(labelsNotInTrainingSet = LabelsNotInTrainingSet)

  // Reports
  private val ReportNoPredictionPartial:
    (LabelsAndInfo[Label]) => Subvalue[RootedTree[Any, Map[Label, Double]], Nothing] =
    reportNoPrediction(
      modelId   = ModelId(),
      _: LabelsAndInfo[Label],
      auditor   = Auditor
    )
  private val ReportNoPredictionEmpty: Subvalue[RootedTree[Any, Map[Label, Double]], Nothing] =
    ReportNoPredictionPartial(LabelsAndInfoEmpty)

  // Throwable and stack trace
  private def getThrowable(errorMessage: String): (Throwable, String) = {
    val throwable = new Exception(errorMessage)
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    throwable.printStackTrace(pw)
    val stackTrace = sw.toString.split("\n").take(NumLinesToKeepInStackTrace).mkString("\n")
    (throwable, stackTrace)
  }

  // Label extractors
  private def extractLabelsOutOfExample(example: Map[String, String]): sci.IndexedSeq[String] =
    example.filterKeys(_.startsWith("label")).toSeq.unzip._2.toIndexedSeq

  private val LabelsOfInterestExtractor = GenFunc0("", extractLabelsOutOfExample)
}
