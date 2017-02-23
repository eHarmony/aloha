package com.eharmony.aloha.models

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.semantics.NoSemantics
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class ErrorModelParserTest {
    private val factory = ModelFactory.defaultFactory(NoSemantics[String](), OptionAuditor[Int]())

  @Test def testErrorsFieldMissing() {
    val js =
      """
        |{
        |  "modelType": "Error",
        |  "modelId": {"id":0, "name": ""}
        |}
      """.stripMargin

    val m = factory.fromString(js)
    assertTrue(m.isSuccess)
  }

  @Test def test0Errors() {
    val js =
      """
        |{
        |  "modelType": "Error",
        |  "modelId": {"id":0, "name": ""},
        |  "errors": []
        |}
      """.stripMargin

    val m = factory.fromString(js)
    assertTrue(m.isSuccess)
  }

  @Test def test1Error() {
    val js =
      """
        |{
        |  "modelType": "Error",
        |  "modelId": {"id":0, "name": ""},
        |  "errors": [
        |    "error 1"
        |  ]
        |}
      """.stripMargin

    val m = factory.fromString(js)
    assertTrue(m.isSuccess)
  }

  @Test def test2Errors() {
    val js =
      """
        |{
        |  "modelType": "Error",
        |  "modelId": {"id":0, "name": ""},
        |  "errors": [
        |    "error 1",
        |    "error 2"
        |  ]
        |}
      """.stripMargin

    val m = factory.fromString(js)
    assertTrue(m.isSuccess)
  }
}
