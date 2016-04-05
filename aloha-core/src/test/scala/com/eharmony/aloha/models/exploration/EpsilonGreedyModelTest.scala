package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.{AnySemanticsWithoutFunctionCreation, CloserTesterModel, ConstantModel}
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.func.GenFunc0
import com.mwt.utilities.PRG
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
    val seed = 1L

    val epsilon = 1f
    val m = makeModel(1, epsilon, seed)

    // This is because we want to mimic the process inside MWT's epsilon greedy explorer
    // so that if something changes, we can become aware.
    val random = new PRG(seed)
    random.uniformUnitInterval()
    val action = random.uniformInt(1, m.classLabels.size)

    val s = m.getScore(null)
    val score = s._2.get
    assertEquals(epsilon / m.classLabels.size, score.getScore.getProbability, delta)
    assertEquals(m.classLabels(action - 1), s._1.right.get)
    assertEquals("b", s._1.right.get)

    // The lack of subscores indicates that the default policy was NOT recorded when doing exploration.
    assertTrue(score.getSubScoresList.isEmpty)
  }

  @Test def policy() {
    val epsilon = 0f
    val m = makeModel(1, epsilon, 0)
    val s = m.getScore(null)

    val score = s._2.get
    assertEquals(1 - epsilon + epsilon / m.classLabels.size, score.getScore.getProbability, delta)
    assertEquals("a", s._1.right.get)
    assertEquals(1, score.getSubScoresCount)
    assertEquals("defaultPolicy", score.getSubScores(0).getScore.getModel.getName)
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
        |   "modelId": {"id": 1, "name": "defaultPolicy"},
        |   "value": $policyValue
        | },
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin.parseJson
    reader.read(js)
  }
}
