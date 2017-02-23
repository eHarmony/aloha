package com.eharmony.aloha.models

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.{OptionAuditor, TreeAuditor}
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.semantics.NoSemantics
import org.junit.Assert.{assertEquals, assertNotNull, assertTrue}
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class ErrorModelTest extends ModelSerializationTestHelper {

  private val factory = ModelFactory.defaultFactory(NoSemantics[Unit](), OptionAuditor[Byte]())

  @Test def test1() {
    val em = ErrorModel(ModelId(), Seq("There should be a valid user ID.  Couldn't find one...", "blah blah"), TreeAuditor[Byte]())
    val s = em(null)
    assertNotNull(s)
    assertTrue(s.value.isEmpty)
  }

  @Test def testEmptyErrors() {

    val json =
      """
        |{
        |  "modelType": "Error",
        |  "modelId": { "id": 0, "name": "" }
        |}
      """.stripMargin

    val m1 = factory.fromString(json)
    assertTrue(m1.isSuccess)

    val json2 =
      """
        |{
        |  "modelType": "Error",
        |  "modelId": { "id": 0, "name": "" },
        |  "errors": []
        |}
      """.stripMargin


    val m2 = factory.fromString(json2)
    assertTrue(m2.isSuccess)
  }

  @Test def testSerialization(): Unit = {
    val m = ErrorModel(ModelId(2, "abc"), Seq("def", "ghi"), OptionAuditor[Byte]())
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)
  }
}
