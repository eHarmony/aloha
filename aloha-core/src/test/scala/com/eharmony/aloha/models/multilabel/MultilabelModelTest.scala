package com.eharmony.aloha.models.multilabel

import java.io.{PrintWriter, StringWriter}

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.tree.RootedTreeAuditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.semantics.func.GenAggFunc
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

  // TODO: Fill in the test implementation and delete comments once done.

  @Test def testSerialization(): Unit = {
    // Assuming all parameters passed to the MultilabelModel constructor are
    // Serializable, MultilabelModel should also be Serializable.

    val modelRoundTrip = serializeDeserializeRoundTrip(modelNoFeatures)
    assertEquals(modelNoFeatures, modelRoundTrip)
  }

//  @Test def testModelCloseClosesPredictor(): Unit = {
//    // Make the predictorProducer passed to the constructor be a
//    //   'SparsePredictorProducer[K] with Closeable'.
//    // predictorProducer should track whether it is closed (using an AtomicBoolean or something).
//    // Call close on the MultilabelModel instance and ensure that the underlying predictor is
//    // also closed.
//
//    fail()
//  }
//
//  @Test def testLabelsOfInterestOmitted(): Unit = {
//    // Test labelsAndInfo[A, K] function.
//    //
//    // When labelsOfInterest = None, labelsAndInfo should return:
//    //   LabelsAndInfo[K](
//    //     indices = labelsInTrainingSet.indices,
//    //     labels = labelsInTrainingSet,
//    //     missingLabels = Seq.empty[K],
//    //     problems = None
//    //   )
//
//    fail()
//  }
//
//  @Test def testLabelsOfInterestProvided(): Unit = {
//    // Test labelsAndInfo[A, K] function.
//    //
//    // labelsAndInfo(a, labelsInTrainingSet, labelsOfInterest, labelToInd) ==
//    // labelsForPrediction(a, labelsOfInterest.get, labelToInd)
//
//    fail()
//  }
//
//  @Test def testReportTooManyMissing(): Unit = {
//    // Make sure Subvalue.natural == None
//    // Check the values of Subvalue.audited and make sure they are as expected.
//    // Subvalue.audited.value should be None.  Check the errors and missing values.
//
//    fail()
//  }
//
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
    assertEquals(Vector(NoLabelsError), report.audited.errorMsgs.take(1))
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

    // TODO: look into problems
    assertEquals(Vector(NoLabelsError) ++ errorMessages, report.audited.errorMsgs)
    assertEquals(None, report.audited.value)
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

    // TODO: refactor this?
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

//  @Test def testLabelsForPredictionContainsProblemsWhenLabelsIsEmpty(): Unit = {
//    // Test this:
//    //    val problems =
//    //      if (labelsShouldPredict.nonEmpty) None
//    //      else Option(labelsOfInterest.accessorOutputProblems(a))
//
//    fail()
//  }
//
//  @Test def testLabelsForPredictionProvidesLabelsThatCantBePredicted(): Unit = {
//    // Test this:
//    //    val noPrediction =
//    //      if (unsorted.size == labelsShouldPredict.size) Seq.empty
//    //      else labelsShouldPredict.filterNot(labelToInd.contains)
//
//    fail()
//  }
//
//  @Test def testLabelsForPredictionReturnsLabelsSortedByIndex(): Unit = {
//    // Test this:
//    //    val (ind, lab) = unsorted.sortBy{ case (i, _) => i }.unzip
//
//    fail()
//  }
//
//  @Test def testSubvalueReportsNoPredictionWhenNoLabelsAreProvided(): Unit = {
//    // Test this:
//    //    if (li.labels.isEmpty)
//    //      reportNoPrediction(modelId, li, auditor)
//
//    fail()
//  }
//
//  @Test def testSubvalueReportsTooManyMissingWhenThereAreTooManyMissingFeatures(): Unit = {
//    // When the amount of missing data exceeds the threshold, reportTooManyMissing should be
//    // called and its value should be returned.  Instantiate a MultilabelModel and
//    // call apply with some missing data required by the features.
//
//    fail()
//  }
//
//  @Test def testExceptionsThrownByPredictorAreHandledGracefully(): Unit = {
//    // Create a predictorProducer that throws.  Check that the model still returns a value
//    // and that the error message is incorporated appropriately.
//
//    fail()
//  }
//
//  @Test def testSubvalueSuccess(): Unit = {
//    // Test the happy path by calling model.apply.  Check the value, missing data, and error messages.
//
//    fail()
//  }
//
//  @Test def testExceptionsThrownInFeatureFunctionsAreNotCaught(): Unit = {
//    // NOTE: This is by design.
//
//    fail()
//  }
}

object MultilabelModelTest {
  // TODO: Use this label type and Auditor.

  private type Label = String
  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]]()

  case class ConstantPredictor[K](prediction: Double = 0d) extends SparseMultiLabelPredictor[K] {
    override def apply(v1: SparseFeatures,
      v2: Labels[K],
      v3: LabelIndices,
      v4: SparseLabelDepFeatures): Map[K, Double] = v2.map(_ -> prediction).toMap
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

  val aud = RootedTreeAuditor[Any, Map[Label, Double]]()
  // private val failure = aud.failure()

  val baseErrorMessage = Stream.continually("Label not in training labels: ")
  val errorMessages = baseErrorMessage.zip(missingLabels).map{ case(msg, label) => s"$msg$label" }

// TODO: Access information returned in audited value by using the following functions:
  //    val aud: RootedTree[Any, Map[Label, Double]] = ???
  //    aud.modelId           // : ModelIdentity
  //    aud.value             // : Option[Map[Label, Double]]  // Should be missing on failure.
  //    aud.missingVarNames   // : Set[String]
  //    aud.errorMsgs         // : Seq[String]
  //    aud.prob              // : Option[Float]  (Shouldn't need this)
}
