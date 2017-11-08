package com.eharmony.aloha.cli

import com.eharmony.aloha.factory.ModelFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner


/**
  * Get the full list of models and compare against a list of all known model types.  This test is here b/c this
  * module depends on the others to it'll have the most complete list.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ModelTypesTest {
  @Test def testKnownModels(): Unit = {
    val expected = Seq(
      "BootstrapExploration",
      "CategoricalDistribution",
      "CloserTester",              // A test model.
      "Constant",
      "DecisionTree",
      "DoubleToLong",
      "EpsilonGreedyExploration",
      "Error",
      "ErrorSwallowingModel",
      "H2o",
      "ModelDecisionTree",
      "Regression",
      "Segmentation",
      "SparseMultilabel",
      "VwJNI"
    )

    val actual = ModelFactory.defaultFactory(null, null).parsers.map(_.modelType).sorted
    assertEquals(expected, actual)
  }
}
