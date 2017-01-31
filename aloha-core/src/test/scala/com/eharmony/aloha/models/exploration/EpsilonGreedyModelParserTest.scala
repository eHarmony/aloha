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
class EpsilonGreedyModelParserTest {
  private[this] val factory = NewModelFactory.defaultFactory(AnySemanticsWithoutFunctionCreation, OptionAuditor[String]())

  @Test def goodModel() {
    val js =
    s"""
      |{
      | "modelType": "EpsilonGreedyExploration",
      | "modelId": {"id": 0, "name": ""},
      | "epsilon": 0.1,
      | "salt": "0",
      | "defaultPolicy": {
      |   "modelType": "Constant",
      |   "modelId": {"id": 1, "name": ""},
      |   "value": 1
      | },
      | "classLabels": ["a", "b", "c"]
      |}
    """.stripMargin

    val m = factory.fromString(js).get
    val s = m(null)
    assertEquals(Option("a"), s)
  }

  @Test(expected = classOf[DeserializationException]) def noClassLabels() {
    val js =
      """
        |{
        | "modelType": "EpsilonGreedyExploration",
        | "modelId": {"id": 0, "name": ""},
        | "epsilon": 0.1,
        | "salt": 0,
        | "defaultPolicy": {
        |   "modelType": "Constant",
        |   "modelId": {"id": 1, "name": ""},
        |   "value": 1
        | }
        |}
      """.stripMargin

    factory.fromString(js).get
  }

  @Test(expected = classOf[DeserializationException]) def noEpsilon() {
    val js =
      """
        |{
        | "modelType": "EpsilonGreedyExploration",
        | "modelId": {"id": 0, "name": ""},
        | "salt": 0,
        | "defaultPolicy": {
        |   "modelType": "Constant",
        |   "modelId": {"id": 1, "name": ""},
        |   "value": 1
        | },
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin

    factory.fromString(js).get
  }

  @Test(expected = classOf[DeserializationException]) def noSalt() {
    val js =
      """
        |{
        | "modelType": "EpsilonGreedyExploration",
        | "modelId": {"id": 0, "name": ""},
        | "epsilon": 0.1,
        | "defaultPolicy": {
        |   "modelType": "Constant",
        |   "modelId": {"id": 1, "name": ""},
        |   "value": 1
        | },
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin

    factory.fromString(js).get
  }

  @Test(expected = classOf[DeserializationException]) def noDefaultPolicy() {
    val js =
      """
        |{
        | "modelType": "EpsilonGreedyExploration",
        | "modelId": {"id": 0, "name": ""},
        | "epsilon": 0.1,
        | "classLabels": ["a", "b", "c"]
        |}
      """.stripMargin

    factory.fromString(js).get
  }
}
