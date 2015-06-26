package com.eharmony.aloha.models

import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner
import org.junit.Test
import org.junit.Assert._

import spray.json._
import spray.json.DefaultJsonProtocol._

import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
import com.eharmony.aloha.factory.ex.AlohaFactoryException

@RunWith(classOf[JUnit4ClassRunner])
class ConstantModelParserTest {


    @Test def testValueOnly() {
        val js =
            """
              |{
              |  "modelType": "Constant",
              |  "modelId": {"id": 0, "name": ""},
              |  "value": 1
              |}
            """.stripMargin.parseJson

        val f = ModelFactory(ConstantModel.parser)
        val m = f.getModel[String, Int](js).get
        val s = m(null)
        assertEquals(1, s.get)
    }

    @Test(expected = classOf[Exception])
    def testNoOutputSpecified() {
        val js =
            """
              |{
              |  "modelType": "Constant",
              |  "modelId": {"id": 0, "name": ""}
              |}
            """.stripMargin.parseJson

        val f = ModelFactory(ConstantModel.parser)
        val m = f.getModel[String, Int](js).get
    }

    @Test(expected = classOf[Exception])
    def testNoModelIdSpecified() {
        val js =
            """
              |{
              |  "modelType": "Constant",
              |  "value": 1
              |}
            """.stripMargin.parseJson

        val f = ModelFactory(ConstantModel.parser)
        val m = f.getModel[String, Int](js).get
    }

    @Test(expected = classOf[AlohaFactoryException])
    def testNothingSpecified() {
        val js =
            """
              |{
              |}
            """.stripMargin.parseJson

        val f = ModelFactory(ConstantModel.parser)
        val m = f.getModel[String, Int](js).get
    }
}
