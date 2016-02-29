package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.models.{AnySemanticsWithoutFunctionCreation, ConstantModel}
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.StringScoreConverter
import org.junit.Assert._
import org.junit.Test
import spray.json._
import spray.json.DefaultJsonProtocol._

/**
  * Created by jmorra on 2/26/16.
  */
class BootstrapModelParserTest {
  private[this] val reader = BootstrapModel.Parser.modelJsonReader[Any, String](ModelFactory(ConstantModel.parser), Option(AnySemanticsWithoutFunctionCreation))

  @Test def goodModel() {
    val js =
      """
        |{
        | "modelType": "BootstrapExploration",
        | "modelId": {"id": 0, "name": ""},
        | "salt": "0",
        | "policies": [
        |   {
        |     "modelType": "Constant",
        |     "modelId": {"id": 1, "name": ""},
        |     "value": 1
        |   },
        |   {
        |     "modelType": "Constant",
        |     "modelId": {"id": 2, "name": ""},
        |     "value": 2
        |   }
        | ],
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin.parseJson

    val m = reader.read(js)
    val s = m(null)
    assertEquals("b", s.get)
  }

  @Test(expected = classOf[DeserializationException]) def noClassLabels() {
    val js =
      """
        |{
        | "modelType": "BootstrapExploration",
        | "modelId": {"id": 0, "name": ""},
        | "salt": 0,
        | "policies": [
        |   {
        |     "modelType": "Constant",
        |     "modelId": {"id": 1, "name": ""},
        |     "value": 1
        |   },
        |   {
        |     "modelType": "Constant",
        |     "modelId": {"id": 2, "name": ""},
        |     "value": 2
        |   }
        | ]
        |}
      """.stripMargin.parseJson

    reader.read(js)
  }

  @Test(expected = classOf[DeserializationException]) def noSalt() {
    val js =
      """
        |{
        | "modelType": "BootstrapExploration",
        | "modelId": {"id": 0, "name": ""},
        | "policies": [
        |   {
        |     "modelType": "Constant",
        |     "modelId": {"id": 1, "name": ""},
        |     "value": 1
        |   },
        |   {
        |     "modelType": "Constant",
        |     "modelId": {"id": 2, "name": ""},
        |     "value": 2
        |   }
        | ],
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin.parseJson

    reader.read(js)
  }

  @Test(expected = classOf[DeserializationException]) def noPolicies() {
    val js =
      """
        |{
        | "modelType": "BootstrapExploration",
        | "modelId": {"id": 0, "name": ""},
        | "salt": 0,
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin.parseJson

    reader.read(js)
  }
}
