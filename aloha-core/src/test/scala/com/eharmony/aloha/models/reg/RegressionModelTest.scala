package com.eharmony.aloha.models.reg

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.semantics.func.GenFunc
import org.junit.Test
import org.junit.Assert.assertEquals
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.DoubleScoreConverter

/**
  * Created by ryan on 12/7/15.
  */
class RegressionModelTest extends ModelSerializationTestHelper {
  import com.eharmony.aloha.models.reg.RegressionModelTest._

  @Test def testSerialization(): Unit = {
    val m = RegressionModel[Any, Double](
              modelId = ModelId(2, "2"),
              featureNames = Vector("empty"),
              featureFunctions = Vector(GenFunc.f0("", Empty)),
              beta = PolynomialEvaluator.builder.result(),
              invLinkFunction = Identity(),
              Option(ConstantDeltaSpline(0, 1, Vector(0, 1))),
              Option(1))

    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)
  }
}

object RegressionModelTest {
  case class Identity[A]() extends (A => A) {
    def apply(a: A) = a
  }

  case object Empty extends (Any => Iterable[(String, Double)]) {
    def apply(a: Any) = Iterable.empty
  }
}