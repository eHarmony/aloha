package com.eharmony.aloha.factory

import java.{lang => jl}
import spray.json._
import spray.json.DefaultJsonProtocol.IntJsonFormat
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
import com.eharmony.aloha.models.{ConstantModel, ErrorModel}


object Values {
    def errorModelInfo1 = {
        val json =
            """
              |{
              |  "modelType": "Error",
              |  "modelId": {"id": 0, "name": "0"},
              |  "errors": [ "error 1", "error 2" ]
              |}
            """.stripMargin.trim

        val mf = ModelFactory(ErrorModel.parser).toTypedFactory[String, Int]
        mf.modelAndInfo.fromString(json).get
    }

    def errorModel1 = errorModelInfo1.model

    def constantModelInfo1 = {
        val json =
            """
              |{
              |  "modelType": "Constant",
              |  "modelId": {"id": 1, "name": "1"},
              |  "value": 42
              |}
            """.stripMargin.trim

        val mf = ModelFactory(ConstantModel.parser).toTypedFactory[String, Int]
        val m = mf.modelAndInfo.fromString(json).get
        m
    }
}
