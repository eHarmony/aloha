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
    val m = makeModel(Seq(1, 2, 3), 0)
    val s = m.getScore(null)
    assertEquals(s._2.get.getScore.getProbability, 1f / 3, delta)
    assertEquals(s._1.right.get, "a")
  }

  @Test def saltOne() {
    val m = makeModel(Seq(1, 2, 3), 1)
    val s = m.getScore(null)
    assertEquals(s._2.get.getScore.getProbability, 1f / 3, delta)
    assertEquals(s._1.right.get, "b")
  }

  @Test def saltTwo() {
    val m = makeModel(Seq(1, 2, 3), 2)
    val s = m.getScore(null)
    assertEquals(s._2.get.getScore.getProbability, 1f / 3, delta)
    assertEquals(s._1.right.get, "c")
  }

  def makeModel(policies: Iterable[Int], salt: Long): BootstrapModel[Any, String] = {
    val policyJss = policies.zipWithIndex.map{ p =>
      s"""
        | {
        |   "modelType": "Constant",
        |   "modelId": {"id": ${p._1 + 1}, "name": ""},
        |   "value": ${p._2 + 1}
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
