package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.NewModelFactory
import com.eharmony.aloha.models.AnySemanticsWithoutFunctionCreation
import org.junit.Assert._
import org.junit.Test
import spray.json._

/**
  * Created by jmorra on 2/26/16.
  */
class BootstrapModelParserTest {
  private[this] val factory = NewModelFactory.defaultFactory(AnySemanticsWithoutFunctionCreation, OptionAuditor[String]())

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
      """.stripMargin


    val m = factory.fromString(js).get
    val s = m(null)
    assertEquals(Option("b"), s)
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
      """.stripMargin

    factory.fromString(js).get
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
      """.stripMargin

    factory.fromString(js).get
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
      """.stripMargin

    factory.fromString(js).get
  }
}
