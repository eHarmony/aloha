package com.eharmony.matching.aloha.models

import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import org.junit.Test

import spray.json._
import spray.json.DefaultJsonProtocol._

import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.matching.aloha.factory.ModelFactory

@RunWith(classOf[JUnit4ClassRunner])
class ErrorModelParserTest {
    @Test def testErrorsFieldMissing() {
        val js =
            """
              |{
              |  "modelType": "Error",
              |  "modelId": {"id":0, "name": ""}
              |}
            """.stripMargin.asJson

        val p = ModelFactory(ErrorModel.parser)
        val m = p.getModel[String, Int](js).get
        val a = 1


    }

    @Test def test0Errors() {
        val js =
            """
              |{
              |  "modelType": "Error",
              |  "modelId": {"id":0, "name": ""},
              |  "errors": []
              |}
            """.stripMargin.asJson

        val p = ModelFactory(ErrorModel.parser)
        val m = p.getModel[String, Int](js).get
        val a = 1
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
            """.stripMargin.asJson

        val p = ModelFactory(ErrorModel.parser)
        val m = p.getModel[String, Int](js).get
        val a = 1


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
            """.stripMargin.asJson

        val p = ModelFactory(ErrorModel.parser)
        val m = p.getModel[String, Int](js).get
        val a = 1


    }
}
