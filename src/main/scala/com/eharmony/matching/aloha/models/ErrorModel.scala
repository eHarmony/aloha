package com.eharmony.matching.aloha.models

import scala.language.higherKinds

import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.factory.{ModelParser, BasicModelParser, ParserProviderCompanion}
import spray.json.{DeserializationException, JsValue, JsonReader}
import com.eharmony.matching.aloha.score.conversions.ScoreConverter


case class ErrorModel(modelId: ModelIdentity, errors: Seq[String]) extends Model[Any, Nothing] {
    private[this] val (w, wo) = {
        val Seq(_w, _wo) = Seq(true, false) map { audit => failure(errors)(audit) }
        (_w, _wo)
    }
    private[aloha] def getScore(a: Any)(implicit audit: Boolean) = if (audit) w else wo
    override def toString() = "ErrorModel(" + modelId + "," + errors + ")"
}

object ErrorModel extends ParserProviderCompanion {
    object Parser extends BasicModelParser {
        val modelType = "Error"
        private[this] val errorField = "errors"
        def modelJsonReader[A, B: JsonReader : ScoreConverter]: JsonReader[ErrorModel] = new JsonReader[ErrorModel] {
            def read(json: JsValue) = {
                val model = for {
                    errors <- json.sa(errorField) orElse { Option(Seq("Error with unspecified reason.")) }
                    mId <- getModelId(json)
                } yield ErrorModel(mId, errors)

                model getOrElse { throw new DeserializationException("")}
            }
        }
    }

    def parser: ModelParser = Parser
}
