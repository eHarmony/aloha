package com.eharmony.aloha.models

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.audit.impl.scoreproto.ScoreAuditor
import com.eharmony.aloha.ex.SchrodingerException
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.semantics.{FunctionWithErrorProducingSemantics, SemanticsUdfException}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.util.Try

/** Test that the ErrorSwallowingModel is very tolerant against failure.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ErrorSwallowingModelTest extends ModelSerializationTestHelper {

  @Test def testSerialization(): Unit = {
    val sub = ErrorModel(ModelId(2, "abc"), Seq("def", "ghi"), OptionAuditor[Byte]())
    val m: ErrorSwallowingModel[Option[_], Byte, Any, Option[Byte]] = ErrorSwallowingModel(sub, OptionAuditor[Byte]())
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)
  }

  @Test def testNoExOnClose(): Unit = {
    val sub = new CloserTesterModel(ModelId(), OptionAuditor[Int](), true)
    val m = new ErrorSwallowingModel(sub, OptionAuditor[Int]())
    try {
      m.close()
    }
    catch {
      case e: Throwable => fail(s"Should not throw a Throwable in the close method. Threw $e")
    }
    assertTrue(sub.isClosed)
  }

  @Test def testExThrownInGetMessage() {
    val json =
      """
        |{
        |  "modelType": "ErrorSwallowingModel",
        |  "submodel": {
        |    "modelType": "Regression",
        |    "modelId": { "id": 0, "name": "" },
        |    "features": {
        |      "feature": "Doesn't matter"
        |    },
        |    "weights": {}
        |  }
        |}
      """.stripMargin
    val semantics = FunctionWithErrorProducingSemantics[Any](new SchrodingerException())
    val factory = ModelFactory.defaultFactory(semantics, ScoreAuditor.doubleAuditor)
    val model = factory.fromString(json).get

    val s = model(null)

    assertFalse("No score should exist", s.hasScore)
    assertTrue("No subscores should exist", s.getSubScoresCount == 0)
    assertEquals("model id", 0, s.getError.getModel.getId)
    assertEquals("model name", "", s.getError.getModel.getName)
    assertEquals("Error count", 3, s.getError.getMessagesCount)
    assertEquals("1st error", "com.eharmony.aloha.ex.SchrodingerException thrown in model 0.", s.getError.getMessages(0))
    assertEquals("2nd error", "exception getMessage function threw exception.  Message Omitted.", s.getError.getMessages(1))
    assertEquals("3rd error", "Stack trace omitted.", s.getError.getMessages(2))
  }

  @Test def testBasicException() {
    val json =
      """
        |{
        |  "modelType": "ErrorSwallowingModel",
        |  "submodel": {
        |    "modelType": "Regression",
        |    "modelId": { "id": 0, "name": "" },
        |    "features": {
        |      "feature": "Doesn't matter"
        |    },
        |    "weights": {}
        |  }
        |}
      """.stripMargin
    val semantics = FunctionWithErrorProducingSemantics[Any](new Exception("exception here."))
    val factory = ModelFactory.defaultFactory(semantics, ScoreAuditor.doubleAuditor)
    val model = factory.fromString(json).get

    val s = model(null)

    assertFalse("No score should exist", s.hasScore)
    assertTrue("No subscores should exist", s.getSubScoresCount == 0)
    assertEquals("model id", 0, s.getError.getModel.getId)
    assertEquals("model name", "", s.getError.getModel.getName)
    assertEquals("Error count", 3, s.getError.getMessagesCount)
    assertEquals("1st error", "java.lang.Exception thrown in model 0.", s.getError.getMessages(0))
    assertEquals("2nd error", "exception here.", s.getError.getMessages(1))
  }

  @Test def testSemanticsUDFException() {
    val json =
      """
        |{
        |  "modelType": "ErrorSwallowingModel",
        |  "submodel": {
        |    "modelType": "Regression",
        |    "modelId": { "id": 0, "name": "" },
        |    "features": {
        |      "feature": "ind(${profile.user_id} < 10)"
        |    },
        |    "weights": {}
        |  }
        |}
      """.stripMargin

    val in = new Object
    val cause = new NullPointerException

    val ex = new SemanticsUdfException[Any](
      "ind(${profile.user_id} < 10)",           // specification
      Map("feature" -> Try { throw cause }),    // accessorOutput
      List(),                                   // accessorsMissingOutput
      List("profile.user_id"),                  // accessorsinErr
      cause,                                    // cause
      in                                        // input
    )

    val semantics = FunctionWithErrorProducingSemantics[Any](ex)
    val factory = ModelFactory.defaultFactory(semantics, ScoreAuditor.doubleAuditor)
    val model = factory.fromString(json).get

    val s = model(null)

    assertFalse("No score should exist", s.hasScore)
    assertTrue("No subscores should exist", s.getSubScoresCount == 0)
    assertEquals("model id", 0, s.getError.getModel.getId)
    assertEquals("model name", "", s.getError.getModel.getName)
    assertEquals("Error count", 6, s.getError.getMessagesCount)
    assertEquals("1st error", "com.eharmony.aloha.semantics.SemanticsUdfException thrown in model 0.", s.getError.getMessages(0))
    assertEquals("4th error", "specification in error: ind(${profile.user_id} < 10)", s.getError.getMessages(3))
    assertEquals("5th error", "accessors in error: profile.user_id", s.getError.getMessages(4))
    assertEquals("6th error", "accessors missing output: ", s.getError.getMessages(5))
  }

  @Test def testSemanticsUDFExceptionWithAllNull() {
    val json =
      """
        |{
        |  "modelType": "ErrorSwallowingModel",
        |  "submodel": {
        |    "modelType": "Regression",
        |    "modelId": { "id": 0, "name": "" },
        |    "features": {
        |      "feature": "ind(${profile.user_id} < 10)"
        |    },
        |    "weights": {}
        |  }
        |}
      """.stripMargin

    val ex = new SemanticsUdfException[Any](null, null, null, null, null, null)

    val semantics = FunctionWithErrorProducingSemantics[Any](ex)
    val factory = ModelFactory.defaultFactory(semantics, ScoreAuditor.doubleAuditor)
    val model = factory.fromString(json).get

    val s = model(null)

    assertFalse("No score should exist", s.hasScore)
    assertTrue("No subscores should exist", s.getSubScoresCount == 0)
    assertEquals("model id", 0, s.getError.getModel.getId)
    assertEquals("model name", "", s.getError.getModel.getName)
    assertEquals("Error count", 6, s.getError.getMessagesCount)
    assertEquals("1st error", "com.eharmony.aloha.semantics.SemanticsUdfException thrown in model 0.", s.getError.getMessages(0))
    assertEquals("4th error", "no specification provided", s.getError.getMessages(3))
    assertEquals("5th error", "no accessorsInErr provided", s.getError.getMessages(4))
    assertEquals("6th error", "no accessorsMissingOutput provided", s.getError.getMessages(5))
  }
}
