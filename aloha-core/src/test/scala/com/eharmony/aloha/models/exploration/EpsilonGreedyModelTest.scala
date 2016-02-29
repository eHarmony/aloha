package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.{AnySemanticsWithoutFunctionCreation, CloserTesterModel, ConstantModel}
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.func.GenFunc0
import org.junit.Assert._
import org.junit.Test
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.{immutable => sci}

/**
  * Created by jmorra on 2/26/16.
  */
class EpsilonGreedyModelTest extends ModelSerializationTestHelper {
  private[this] val reader = EpsilonGreedyModel.Parser.modelJsonReader[Any, String](ModelFactory(ConstantModel.parser), Option(AnySemanticsWithoutFunctionCreation))
  private[this] val delta = 0.00001f
  implicit val audit = true

  @Test def testSerialization() {
    val constantPolicy = ConstantModel(Right(1), ModelId(2, "abc"))
    val m = EpsilonGreedyModel(ModelId(3, "def"), constantPolicy, 0.1f, null, sci.IndexedSeq(1, 2, 3))
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)

    val m2 = EpsilonGreedyModel(ModelId(3, "def"), constantPolicy, 0.1f, null, sci.IndexedSeq("1", "2", "3"))
    val m3 = serializeDeserializeRoundTrip(m2)
    assertEquals(m2, m3)
  }

  @Test def testClosed() {
    val sub = new CloserTesterModel[Int]()
    EpsilonGreedyModel(ModelId.empty, sub, 0.1f, GenFunc0("", (s: String) => 1l), sci.IndexedSeq(1, 2)).close()
    assertTrue(sub.isClosed)
  }

  @Test def random() {
    val epsilon = 0.9f
    val m = makeModel(1, epsilon, 1)
    val s = m.getScore(null)
    assertEquals(s._2.get.getScore.getProbability, epsilon / 3, delta)
    assertEquals(s._1.right.get, "b")
  }

  @Test def policy() {
    val epsilon = 0.1f
    val m = makeModel(1, epsilon, 0)
    val s = m.getScore(null)
    assertEquals(s._2.get.getScore.getProbability, 1 - epsilon + epsilon / 3, delta)
    assertEquals(s._1.right.get, "a")
  }

  def makeModel(policyValue: Int, epsilon: Float, salt: Long): EpsilonGreedyModel[Any, String] = {
    val js =
      s"""
        |{
        | "modelType": "EpsilonGreedyExploration",
        | "modelId": {"id": 0, "name": ""},
        | "epsilon": $epsilon,
        | "salt": "$salt",
        | "defaultPolicy": {
        |   "modelType": "Constant",
        |   "modelId": {"id": 1, "name": ""},
        |   "value": $policyValue
        | },
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin.parseJson
    reader.read(js)
  }
}
