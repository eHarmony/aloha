package com.eharmony.aloha.factory

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.semantics.NoSemantics


object Values {
    def errorModel1: Model[String, Option[Int]] = {
        val json =
            """
              |{
              |  "modelType": "Error",
              |  "modelId": {"id": 0, "name": "0"},
              |  "errors": [ "error 1", "error 2" ]
              |}
            """.stripMargin.trim

        val mf = NewModelFactory.defaultFactory(NoSemantics[String](), OptionAuditor[Int]())
        mf.fromString(json).get
    }

    def constantModel1: Model[String, Option[Int]] = {
        val json =
            """
              |{
              |  "modelType": "Constant",
              |  "modelId": {"id": 1, "name": "1"},
              |  "value": 42
              |}
            """.stripMargin.trim

        val mf = NewModelFactory.defaultFactory(NoSemantics[String](), OptionAuditor[Int]())
        val m = mf.fromString(json).get
        m
    }
}
