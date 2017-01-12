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
}
