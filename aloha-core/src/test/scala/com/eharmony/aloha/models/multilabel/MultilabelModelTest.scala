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
    val report  = reportTooManyMissing(
      modelId   = ModelId(),
      labelInfo = labelsAndInfoEmpty,
      missing   = scm.Map("" -> missingLabels),
      auditor   = auditor
    )

    assertEquals(Vector(TooManyMissingError), report.audited.errorMsgs.take(1))
    assertEquals(None, report.audited.value)
    assertEquals(None, report.natural)
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
      labelsAndInfoEmpty.copy(labelsNotInTrainingSet = missingLabels),
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
      labelInfo  = labelsAndInfoEmpty.copy(labelsNotInTrainingSet = missingLabels),
      missing    = scm.Map("" -> missingLabels),
      prediction = predictions,
      auditor    = Auditor
    )
    assertEquals(Some(predictions), report.natural)
    assertEquals(Some(predictions), report.audited.value)
    assertEquals(report.natural, report.audited.value)
  }

  @Test def testLabelsForPredictionContainsProblemsWhenNoLabelProvided(): Unit = {
    val labelsAndInfoNoLabels = labelsForPrediction(
      example          = Map[Label, Label](),  // no label provided
      labelsOfInterest = labelsOfInterestExtractor,
      labelToInd       = labelsInTrainingSetToIndex)

    // In this scenario no problems are found but an
    // Option(GenAggFuncAccessorProblems) is returned instead of None
    val problemsNoLabelsExpected = Option(GenAggFuncAccessorProblems(Seq(), Seq()))
    assertEquals(problemsNoLabelsExpected, labelsAndInfoNoLabels.problems)
  }

  @Test def testLabelsForPredictionContainsProblemsWhenLabelsIsNotPresent(): Unit = {
    val labelsExtractor =
      (label: Map[Label, Label]) => label.get("labels") match {
        case ls: sci.IndexedSeq[_] => Option(ls.asInstanceOf[sci.IndexedSeq[Label]])
        case _ => None
      }

    val descriptor = "label is missing"
    val labelsOfInterest =
      GenFunc.f1(GeneratedAccessor(descriptor, labelsExtractor, None))(
        "", _ getOrElse sci.IndexedSeq.empty[Label]
      )

    val labelsAndInfoNoLabelsGen1 = labelsForPrediction(
      example          = Map[Label, Label](),
      labelsOfInterest = labelsOfInterest,
      labelToInd       = labelsInTrainingSetToIndex)
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
    Try(
    labelsForPrediction(
      Map[String, String](),
      labelsOfInterestWrapped,
      labelsInTrainingSetToIndex)
    ).failed.get match {
      case SemanticsUdfException(_, _, _, accessorsInErr, _, _) =>
        assertEquals(accessorsInErr, problemsNoLabelsExpected.get.errors)
    }
  }

  @Test def testLabelsForPredictionProvidesLabelsThatCantBePredicted(): Unit = {
    val labelNotInTrainingSet = "d"
    val labelsAndInfo = labelsForPrediction(Map("label" -> labelNotInTrainingSet),
      labelsOfInterestExtractor, labelsInTrainingSetToIndex)
    val missingLabels2 = labelsAndInfo.labelsNotInTrainingSet
    assertEquals(Seq(labelNotInTrainingSet), missingLabels2)
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
    assertEquals(None, modelWithFeatureFunctions.subvalue(Vector.empty).natural)
  }

  @Test def testSubvalueReportsTooManyMissingWhenThereAreTooManyMissingFeatures(): Unit = {
    // When the amount of missing data exceeds the threshold, reportTooManyMissing should be
    // called and its value should be returned.

    val modelWithMissingThreshold  = modelWithFeatureFunctions.copy(
      featureNames = sci.IndexedSeq("feature1"),
      featureFunctions = featureFunctions,
      labelsInTrainingSet = labelsInTrainingSet,
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

    val modelWithThrowingPredictorProducer = modelWithFeatureFunctions.copy(
      predictorProducer = Lazy(PredictorThatThrows),
      labelsOfInterest    = None
    )

    val result = modelWithThrowingPredictorProducer(Vector.empty)
    assertEquals(None, result.value)
    assertEquals("java.lang.Exception: error", result.errorMsgs.head.split("\n").head)
  }

  @Test def testSubvalueSuccess(): Unit = {
    val scoreToReturn = 5d
    val modelSuccess = modelNoFeatures.copy(
      labelsInTrainingSet = sci.IndexedSeq[Label]("label1", "label2", "label3", "label4"),
      labelsOfInterest    = Option(labelsOfInterestExtractor),
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

    val modelWithFeatureFunctionThatThrows = modelNoFeatures.copy(
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

  // Common input
  val labelsInTrainingSet: sci.IndexedSeq[Label] = sci.IndexedSeq[Label]("a", "b", "c")
  val labelsInTrainingSetToIndex: Map[Label, Int] =labelsInTrainingSet.zipWithIndex.toMap
  val missingLabels: Seq[Label] = Seq("a", "b")
  val baseErrorMessage: Seq[String] = Stream.continually("Label not in training labels: ")
  val errorMessages: Seq[String] = baseErrorMessage.zip(missingLabels).map {
    case(msg, label) => s"$msg$label"
  }
  val auditor: RootedTreeAuditor[Any, Map[Label, Double]] =
    RootedTreeAuditor[Any, Map[Label, Double]]()

  // Feature functions
  val EmptyIndicatorFn: GenAggFunc[Map[String, String], Iterable[(String, Double)]] =
    GenFunc0("", _ => Iterable())
  val featureFunctions = Vector(EmptyIndicatorFn)

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

  val modelWithFeatureFunctions:
    MultilabelModel[Tree[Any], String, Vector[String], RootedTree[Any, Map[Label, Double]]] =
    modelNoFeatures.copy(
    featureFunctions    = sci.IndexedSeq[GenAggFunc[Vector[String], Sparse]](),
    labelsInTrainingSet = labelsInTrainingSet,
    labelsOfInterest    = Some(GenFunc0("", (a: Vector[String]) => a))
  )

  // LabelsAndInfo
  val labelsAndInfoEmpty = LabelsAndInfo(
    indices = sci.IndexedSeq.empty,
    labels = sci.IndexedSeq.empty,
    labelsNotInTrainingSet = Seq[Label](),
    problems = None
  )
  val labelsAndInfoMissingLabels: LabelsAndInfo[Label] =
    labelsAndInfoEmpty.copy(labelsNotInTrainingSet = missingLabels)

  // Reports
  val reportNoPredictionPartial:
    (LabelsAndInfo[Label]) => Subvalue[RootedTree[Any, Map[Label, Double]], Nothing] =
    reportNoPrediction(
    modelId   = ModelId(),
    _: LabelsAndInfo[Label],
    auditor   = Auditor
  )
  val reportNoPredictionEmpty: Subvalue[RootedTree[Any, Map[Label, Double]], Nothing] =
    reportNoPredictionPartial(labelsAndInfoEmpty)

  // Throwable and stack trace
  def getThrowable(errorMessage: String): (Throwable, String) = {
    val throwable = Try(throw new Exception(errorMessage)).failed.get
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    throwable.printStackTrace(pw)
    val stackTrace = sw.toString.split("\n").take(NumLinesToKeepInStackTrace).mkString("\n")
    (throwable, stackTrace)
  }

  // Label extractors
  def extractLabelsOutOfExample(example: Map[String, String]): sci.IndexedSeq[String] =
    example.filterKeys(_.startsWith("label")).toSeq.unzip._2.toIndexedSeq

  val labelsOfInterestExtractor = GenFunc0("", extractLabelsOutOfExample)
}
