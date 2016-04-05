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
class BootstrapModelTest extends ModelSerializationTestHelper {
  private[this] val Reader = BootstrapModel.Parser.modelJsonReader[Any, String](ModelFactory(ConstantModel.parser), Option(AnySemanticsWithoutFunctionCreation))
  private[this] val delta = 0.00001f
  implicit val audit = true

  @Test def testSerialization() {
    val polices = sci.IndexedSeq(
      ConstantModel(Right(1), ModelId(1, "abc")),
      ConstantModel(Right(2), ModelId(2, "abc")),
      ConstantModel(Right(1), ModelId(3, "abc"))
    )

    val m = BootstrapModel(ModelId(4, "def"), polices, null, sci.IndexedSeq(1, 2, 3))
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)

    val m2 = BootstrapModel(ModelId(4, "def"), polices, null, sci.IndexedSeq("1", "2", "3"))
    val m3 = serializeDeserializeRoundTrip(m2)
    assertEquals(m2, m3)
  }

  @Test def testClosed() {
    val sub1 = new CloserTesterModel[Int]()
    val sub2 = new CloserTesterModel[Int]()
    val subs = sci.IndexedSeq(sub1, sub2)
    BootstrapModel(ModelId.empty, subs, GenFunc0("", (s: String) => 1l), sci.IndexedSeq(1, 2)).close()
    subs.foreach(s => assertTrue(s.isClosed))
  }

  @Test def saltZero() {
    val m = makeModel(Seq(1, 2, 3, 3), 0)
    val s = m.getScore(null)
    val score = s._2.get
    val subScores = score.getSubScoresList
    assertEquals(score.getScore.getProbability, 0.5f, delta)
    assertEquals(s._1.right.get, "c")
    assertEquals(2, subScores.size)
    assertEquals("model: 3", subScores.get(0).getScore.getModel.getName)
    assertEquals("model: 4", subScores.get(1).getScore.getModel.getName)
  }

  @Test def saltSix() {
    val m = makeModel(Seq(1, 2, 3, 3), 6)
    val s = m.getScore(null)
    val score = s._2.get
    val subScores = score.getSubScoresList
    assertEquals(score.getScore.getProbability, 0.25f, delta)
    assertEquals(s._1.right.get, "a")
    assertEquals(1, subScores.size)
    assertEquals("model: 1", subScores.get(0).getScore.getModel.getName)
  }

  @Test def saltFive() {
    val m = makeModel(Seq(1, 2, 3, 3), 5)
    val s = m.getScore(null)
    val score = s._2.get
    val subScores = score.getSubScoresList
    assertEquals(score.getScore.getProbability, 0.25f, delta)
    assertEquals(s._1.right.get, "b")
    assertEquals(1, subScores.size)
    assertEquals("model: 2", subScores.get(0).getScore.getModel.getName)
  }

  def makeModel(policies: Iterable[Int], salt: Long): BootstrapModel[Any, String] = {
    val policyJss = policies.zipWithIndex.map{ p =>
      s"""
        | {
        |   "modelType": "Constant",
        |   "modelId": {"id": ${p._2 + 1}, "name": "model: ${p._2 + 1}"},
        |   "value": ${p._1}
        | }
      """.stripMargin
    }.mkString(",")
    val js =
      s"""
         |{
         | "modelType": "BootstrapExploration",
         | "modelId": {"id": 0, "name": ""},
         | "salt": "$salt",
         | "policies": [
         |   $policyJss
         | ],
         | "classLabels": ["a", "b", "c"]
         |}
      """.stripMargin.parseJson
    Reader.read(js)
  }
}
