package com.eharmony.aloha.models

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.NewModelFactory
import com.eharmony.aloha.factory.ex.AlohaFactoryException
import com.eharmony.aloha.semantics.NoSemantics
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class ConstantModelParserTest {

  private val factory = NewModelFactory.defaultFactory(NoSemantics[String](), OptionAuditor[Int]())


  @Test def testValueOnly() {
    val js =
      """
        |{
        |  "modelType": "Constant",
        |  "modelId": {"id": 0, "name": ""},
        |  "value": 1
        |}
      """.stripMargin

    val m = factory.fromString(js).get
    val s = m(null)
    assertEquals(Option(1), s)
  }

  @Test(expected = classOf[Exception])
  def testNoOutputSpecified() {
    val js =
      """
        |{
        |  "modelType": "Constant",
        |  "modelId": {"id": 0, "name": ""}
        |}
      """.stripMargin

    val m = factory.fromString(js)
    m.get
  }

  @Test(expected = classOf[Exception])
  def testNoModelIdSpecified() {
    val js =
      """
        |{
        |  "modelType": "Constant",
        |  "value": 1
        |}
      """.stripMargin

    val m = factory.fromString(js)
    m.get
  }

  @Test(expected = classOf[AlohaFactoryException])
  def testNothingSpecified() {
    val js =
      """
        |{
        |}
      """.stripMargin

    val m = factory.fromString(js)
    m.get
  }
}
