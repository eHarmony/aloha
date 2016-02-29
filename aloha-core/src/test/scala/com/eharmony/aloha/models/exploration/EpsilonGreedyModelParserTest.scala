package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.models.{AnySemanticsWithoutFunctionCreation, ConstantModel}
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import org.junit.Assert._
import org.junit.Test
import spray.json._
import spray.json.DefaultJsonProtocol._

/**
  * Created by jmorra on 2/26/16.
  */
class EpsilonGreedyModelParserTest {
  private[this] val reader = EpsilonGreedyModel.Parser.modelJsonReader[Any, String](ModelFactory(ConstantModel.parser), Option(AnySemanticsWithoutFunctionCreation))

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
    """.stripMargin.parseJson

    val m = reader.read(js)
    val s = m(null)
    assertEquals("a", s.get)
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
      """.stripMargin.parseJson

    reader.read(js)
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
      """.stripMargin.parseJson

    reader.read(js)
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
      """.stripMargin.parseJson

    reader.read(js)
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
      """.stripMargin.parseJson

    reader.read(js)
  }
}
