package com.eharmony.aloha.models.multilabel

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.tree.RootedTreeAuditor
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan.deak on 9/1/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class MultilabelModelTest extends ModelSerializationTestHelper {


  // TODO: Fill in the test implementation and delete comments once done.

  @Test def testSerialization(): Unit = {
    // The name of this test needs to be exactly 'testSerialization'.  Don't change.
    // Assuming all parameters passed to the MultilabelModel constructor are
    // Serializable, MultilabelModel should also be Serializable.
    //
    // See com.eharmony.aloha.models.ConstantModelTest.testSerialization()

    fail()
  }

  @Test def testModelCloseClosesPredictor(): Unit = {
    // Make the predictorProducer passed to the constructor be a
    //   'SparsePredictorProducer[K] with Closeable'.
    // predictorProducer should track whether it is closed (using an AtomicBoolean or something).
    // Call close on the MultilabelModel instance and ensure that the underlying predictor is
    // also closed.

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

    fail()
  }

  @Test def testLabelsOfInterestProvided(): Unit = {
    // Test labelsAndInfo[A, K] function.
    //
    // labelsAndInfo(a, labelsInTrainingSet, labelsOfInterest, labelToInd) ==
    // labelsForPrediction(a, labelsOfInterest.get, labelToInd)

    fail()
  }

  @Test def testReportTooManyMissing(): Unit = {
    // Make sure Subvalue.natural == None
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be None.  Check the errors and missing values.

    fail()
  }

  @Test def testReportNoPrediction(): Unit = {
    // Make sure Subvalue.natural == None
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be None.  Check the errors and missing values.

    fail()
  }

  @Test def testReportPredictorError(): Unit = {
    // Make sure Subvalue.natural == None
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be None.  Check the errors and missing values.

    fail()
  }

  @Test def testReportSuccess(): Unit = {
    // Make sure Subvalue.natural == Some(value)
    // Check the values of Subvalue.audited and make sure they are as expected.
    // Subvalue.audited.value should be Some(value2).
    // 'value' should equal 'value2'.
    // Check the errors and missing values.

    fail()
  }

  @Test def testLabelsForPredictionContainsProblemsWhenLabelsIsEmpty(): Unit = {
    // Test this:
    //    val problems =
    //      if (labelsShouldPredict.nonEmpty) None
    //      else Option(labelsOfInterest.accessorOutputProblems(a))

    fail()
  }

  @Test def testLabelsForPredictionProvidesLabelsThatCantBePredicted(): Unit = {
    // Test this:
    //    val noPrediction =
    //      if (unsorted.size == labelsShouldPredict.size) Seq.empty
    //      else labelsShouldPredict.filterNot(labelToInd.contains)

    fail()
  }

  @Test def testLabelsForPredictionReturnsLabelsSortedByIndex(): Unit = {
    // Test this:
    //    val (ind, lab) = unsorted.sortBy{ case (i, _) => i }.unzip

    fail()
  }

  @Test def testSubvalueReportsNoPredictionWhenNoLabelsAreProvided(): Unit = {
    // Test this:
    //    if (li.labels.isEmpty)
    //      reportNoPrediction(modelId, li, auditor)

    fail()
  }

  @Test def testSubvalueReportsTooManyMissingWhenThereAreTooManyMissingFeatures(): Unit = {
    // When the amount of missing data exceeds the threshold, reportTooManyMissing should be
    // called and its value should be returned.  Instantiate a MultilabelModel and
    // call apply with some missing data required by the features.

    fail()
  }

  @Test def testExceptionsThrownByPredictorAreHandledGracefully(): Unit = {
    // Create a predictorProducer that throws.  Check that the model still returns a value
    // and that the error message is incorporated appropriately.

    fail()
  }

  @Test def testSubvalueSuccess(): Unit = {
    // Test the happy path by calling model.apply.  Check the value, missing data, and error messages.

    fail()
  }
}

object MultilabelModelTest {
  // TODO: Use this label type and Auditor.

  private type Label = String
  private val Auditor = RootedTreeAuditor.noUpperBound[Map[Label, Double]](
    accumulateErrors = false,
    accumulateMissingFeatures = false
  )

// TODO: Access information returned in audited value by using the following functions:
  //    val aud: RootedTree[Any, Map[Label, Double]] = ???
  //    aud.modelId           // : ModelIdentity
  //    aud.value             // : Option[Map[Label, Double]]  // Should be missing on failure.
  //    aud.missingVarNames   // : Set[String]
  //    aud.errorMsgs         // : Seq[String]
  //    aud.prob              // : Option[Float]  (Shouldn't need this)
}