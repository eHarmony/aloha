package com.eharmony.aloha.models.reg

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc, GeneratedAccessor}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner


@RunWith(classOf[BlockJUnit4ClassRunner])
class PolynomialEvaluationAlgoTest {
  private[this] val expected = (1 << 7) - 1 // 0111 1111
  private[this] val tolerance = 1.0e-6

  private[this] val accessor = (key: String) => (_:Map[String, String]).get(key).map(k => Seq((k, 1.0))).getOrElse(Nil)
  private[this] val semantics = new Semantics[Map[String, String]] {
    def close(): Unit = {}
    def refInfoA: RefInfo[Map[String, String]] = RefInfo[Map[String, String]]
    def accessorFunctionNames = Nil
    def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Map[String, String], B]] = {
      val acc = GeneratedAccessor(codeSpec, accessor(codeSpec))
      val f = GenFunc.f1(acc)(codeSpec, identity)
      Right(f.asInstanceOf[GenAggFunc[Map[String, String], B]])
    }
  }

  private[this] val factory = ModelFactory.defaultFactory(semantics, OptionAuditor[Double]())

  @Test def testManualPolyEval() {
    val x = IndexedSeq(
      Seq(("intercept",          1.0)),
      Seq(("female_country=1", 1.0)),
      Seq(("male_country=2",   1.0)),
      Seq(("user_gender=MALE",   1.0)),
      Seq(("cand_gender=FEMALE", 1.0))
    )

    val weightPaths = Map[Map[String, Int], Double](
      Map("intercept"        -> 0                           ) -> (1 << 0),
      Map("female_country=1" -> 1                           ) -> (1 << 1),
      Map("male_country=2"   -> 2                           ) -> (1 << 2),
      Map("user_gender=MALE" -> 3                           ) -> (1 << 3),
      Map("female_country=1" -> 1, "user_gender=MALE"   -> 3) -> (1 << 4),
      Map("female_country=1" -> 1, "cand_gender=FEMALE" -> 4) -> (1 << 5),
      Map("male_country=2"   -> 2, "user_gender=MALE"   -> 3) -> (1 << 6)
    )

    val w = (PolynomialEvaluator.builder ++= weightPaths).result()

    val y = w at x
    assertEquals(expected, y, tolerance)
    assertEquals(weightPaths.values.sum, y, tolerance)
  }

  /** The purpose of this test is to ensure that the higher order features are extracted correctly via JSON AND to
    * ensure correct processing once the model is created.  This small model has some of the following necessary
    * features to test all of this:
    *
      1. first order weights that aren't in any higher order interaction
      1. higher order weights that have no associated first order weights
      1. weights that appear in both.
    */
  @Test def testJsonParsedPolyEval() {
    val jStr =
      """
        |{
        |  "modelType": "Regression",
        |  "modelId": { "id": 0, "name": "" },
        |  "features": {
        |    "intercept": "intercept",
        |    "female_country": "female_country",
        |    "male_country": "male_country",
        |    "user_gender": "user_gender",
        |    "cand_gender": "cand_gender"
        |  },
        |  "weights": {
        |    "intercept": 1,
        |    "female_country=1": 2,
        |    "male_country=2": 4,
        |    "user_gender=MALE": 8
        |  },
        |  "higherOrderFeatures": [
        |    { "features": { "female_country": ["female_country=1"], "user_gender": ["user_gender=MALE"] },   "wt": 16 },
        |    { "features": { "female_country": ["female_country=1"], "cand_gender": ["cand_gender=FEMALE"] }, "wt": 32 },
        |    { "features": { "male_country":   ["male_country=2"],   "user_gender": ["user_gender=MALE"] },   "wt": 64 }
        |  ]
        |}
      """.stripMargin.trim

    val m = factory.fromString(jStr).get

    val x = Map(
      "intercept" -> "",
      "female_country" -> "=1",
      "male_country" -> "=2",
      "user_gender" -> "=MALE",
      "cand_gender" -> "=FEMALE"
    )

    val score = m(x)
    assertEquals(expected, score.get, tolerance)
  }
}
