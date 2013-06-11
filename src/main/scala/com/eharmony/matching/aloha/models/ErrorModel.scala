package com.eharmony.matching.aloha.models

import scala.language.higherKinds

import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.NothingScoreConverter
import com.eharmony.matching.aloha.factory.{BasicModelParser, ParserProviderCompanion, ModelParser}
import spray.json.{DeserializationException, JsValue, JsonReader}
import com.eharmony.matching.aloha.score.conversions.ScoreConverter


case class ErrorModel(modelId: ModelIdentity, errors: Seq[String]) extends Model[Any, Nothing] {
    private[this] val (w, wo) = {
        val f = ModelOutput.fail(errors:_*)
        (toScoreTuple(f)(true, NothingScoreConverter), (f, None))
    }
    private[aloha] def getScore(a: Any)(implicit audit: Boolean) = if (audit) w else wo
    override def toString = "ErrorModel(" + modelId + "," + errors + ")"
}

object ErrorModel extends ParserProviderCompanion {
    protected[this] type M[-A, +B] = ErrorModel
    private[this] class Parser extends BasicModelParser[M] {
        val modelType = "Error"
        private[this] val errorField = "errors"
        def modelJsonReader[A, B: JsonReader : ScoreConverter, C >: M[A, B]] = new JsonReader[C] {
            def read(json: JsValue) = {
                val model = for {
                    errors <- json.sa(errorField) orElse { Option(Seq("Error with unspecified reason.")) }
                    mId <- getModelId(json)
                } yield ErrorModel(mId, errors)

                model getOrElse { throw new DeserializationException("")}
            }
        }
    }

    val parser = new Parser().asInstanceOf[ModelParser[M]]
}
