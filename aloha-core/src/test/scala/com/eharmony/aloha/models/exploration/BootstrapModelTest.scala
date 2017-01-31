package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.TreeAuditor.Tree
import com.eharmony.aloha.audit.impl.{OptionAuditor, TreeAuditor}
import com.eharmony.aloha.factory.NewModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.{AnySemanticsWithoutFunctionCreation, CloserTesterModel, ConstantModel, Model}
import com.eharmony.aloha.semantics.func.GenFunc0
import org.junit.Assert._
import org.junit.Test

import scala.collection.{immutable => sci}

/**
  * Created by jmorra on 2/26/16.
  */
class BootstrapModelTest extends ModelSerializationTestHelper {
  private[this] val factory = NewModelFactory.defaultFactory(AnySemanticsWithoutFunctionCreation, TreeAuditor[String]())
//  private[this] val Reader = BootstrapModel.Parser.modelJsonReader[Any, String](ModelFactory(ConstantModel.parser), Option())
  private[this] val delta = 0.00001f
  implicit val audit = true

  @Test def testSerialization() {

    val polices = sci.IndexedSeq(
      ConstantModel(Option(1), ModelId(1, "abc"), OptionAuditor[Int]()),
      ConstantModel(Option(2), ModelId(2, "abc"), OptionAuditor[Int]()),
      ConstantModel(Option(1), ModelId(3, "abc"), OptionAuditor[Int]())
    )

    val m = BootstrapModel(ModelId(4, "def"), polices, null, sci.IndexedSeq(1, 2, 3), OptionAuditor[Int]())
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)

    val m2 = BootstrapModel(ModelId(4, "def"), polices, null, sci.IndexedSeq("1", "2", "3"), OptionAuditor[String]())
    val m3 = serializeDeserializeRoundTrip(m2)
    assertEquals(m2, m3)
  }

  @Test def testClosed() {
    val sub1 = new CloserTesterModel(ModelId(), OptionAuditor[Int]())
    val sub2 = new CloserTesterModel(ModelId(), OptionAuditor[Int]())
    val subs = sci.IndexedSeq(sub1, sub2)
    BootstrapModel(ModelId.empty, subs, GenFunc0("", (s: String) => 1l), sci.IndexedSeq(1, 2), OptionAuditor[Int]()).close()
    subs.foreach(s => assertTrue(s.isClosed))
  }

  // Using salts 0, 5, 6 because they illicit all three actions in this particular case.

  val subPolicies = Seq(1, 2, 3, 3)

  /**
    * This test will create a Bootstrap model with 4 constant policies.  Those policies will return (in order) action
    * 1, 2, 3, and 3.  By setting the salt to 0 we ensure that the explorer chooses EITHER policy 3 or 4.  Because both
    * of those policies return the same action the probability should be 2/4.  We also make sure that the sub scores
    * are recorded for ONLY those policies that had the same action as the one returned by the explorer.
    */
  @Test def saltZero() {
    val m = makeModel(subPolicies, 0)
    val s = m(null)
    assertEquals(0.5f, s.prob.get, delta)
    assertEquals("c", s.value.get)
    assertEquals(2, s.subvalues.size)
    assertEquals("model: 3", s.subvalues.head.modelId.getName())
    assertEquals("model: 4", s.subvalues(1).modelId.getName())
  }

  @Test def saltSix() {
    val m = makeModel(subPolicies, 6)
    val s = m(null)
    assertEquals(0.25f, s.prob.get, delta)
    assertEquals("a", s.value.get)
    assertEquals(1, s.subvalues.size)
    assertEquals("model: 1", s.subvalues.head.modelId.getName())
  }

  @Test def saltFive() {
    val m = makeModel(subPolicies, 5)
    val s = m(null)
    assertEquals(0.25f, s.prob.get, delta)
    assertEquals("b", s.value.get)
    assertEquals(1, s.subvalues)
    assertEquals("model: 2", s.subvalues.head.modelId.getName())
  }

  def makeModel(policies: Iterable[Int], salt: Long): Model[Any, Tree[String]] = {
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
      """.stripMargin
    factory.fromString(js).get
  }
}
