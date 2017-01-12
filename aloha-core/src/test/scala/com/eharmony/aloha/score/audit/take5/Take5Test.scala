package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.score.audit.support.{IntValue, StringValue, Tree}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 1/11/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class Take5Test {
  @Test def testConstantModel(): Unit = {
    val anyInput = ()
    val expected = 1
    val modelId = ModelId(1, "one")
    val constantValue = Option(expected)
    val auditor = TreeAuditor.intTreeAuditor
    val model = ConstantModel(modelId, auditor, constantValue)
    val actual = model(anyInput).value.get.value
    assertEquals(expected, actual)
  }

  @Test def testHierarchicalModel(): Unit = {
    val anyInput = ()
    val cId = ModelId(1, "const")
    val cValue = 1
    val constModel = ConstantModel(cId, TreeAuditor.intTreeAuditor, Option(cValue))

    val hId = ModelId(2, "hier")
    val hValue = "non-negative"
    val hierModel = HierarchicalConstantModel(hId, hValue, constModel, TreeAuditor.stringTreeAuditor)

    val expected = Tree(hId, StringValue(hValue), Seq(Tree(cId, IntValue(cValue))))
    val actual = hierModel(anyInput)
    assertEquals(expected, actual)
  }

  @Test def testFloatModel(): Unit = {
    val anyInput = ()
    val expected = 1.23f
    val modelId = ModelId(1, "one")

    val auditor = new OptionAuditor[Float]
    val model = FloatModel(modelId, expected, auditor)
    val actual = model(anyInput)

    assertEquals(Option(expected), actual)
  }

  @Test def testChangeOptionAuditorType(): Unit = {
    val ia = OptionAuditor[Int]()
    val ofa = ia.changeType[Float]
    assertEquals(Option(OptionAuditor[Float]()), ofa)
  }

  @Test def testChangedOptionAuditorAuditsCorrectly(): Unit = {
    val f = 1.23f
    val ofa = OptionAuditor[Int]().changeType[Float]
    assertEquals(Option(f), ofa.flatMap(a => a.success(null, f, null, null, null)))
  }

  @Test def testChangeTreeAuditorType(): Unit = {
    assertEquals(Option(IntTreeAuditor), IntTreeAuditor.changeType[Int])
    assertEquals(Option(IntTreeAuditor), StringTreeAuditor.changeType[Int])

    assertEquals(Option(StringTreeAuditor), IntTreeAuditor.changeType[String])
    assertEquals(Option(StringTreeAuditor), StringTreeAuditor.changeType[String])

    assertEquals(None, IntTreeAuditor.changeType[Float])
    assertEquals(None, StringTreeAuditor.changeType[Float])
  }

  @Test def testChangedTreeAuditorAuditsCorrectly(): Unit = {
    val id = ModelId(1, "2")
    val valueToAudit = 3
    val v = StringTreeAuditor.changeType[Int].map(a => a.success(id, valueToAudit, Set.empty, Nil, None))
    assertEquals(Option(Tree(id, IntValue(valueToAudit))), v)
  }
}
