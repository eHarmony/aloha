package com.eharmony.matching.aloha.models

import com.eharmony.matching.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.factory.{BasicModelParser, ParserProviderCompanion, ModelParser}
import spray.json.{DeserializationException, JsValue, JsonReader}

case class ConstantModel[+B: ScoreConverter](constant: ModelOutput[B], modelId: ModelIdentity) extends Model[Any, B] {
    private[this] val w = toScoreTuple(constant)(audit = true, implicitly[ScoreConverter[B]])
    private[this] val wo = toScoreTuple(constant)(audit = false, implicitly[ScoreConverter[B]])
    private[aloha] final def getScore(a: Any)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = if (audit) w else wo
}

object ConstantModel extends ParserProviderCompanion {
    def apply[B: ScoreConverter](b: B): ConstantModel[B] = ConstantModel(ModelOutput(b), ModelId())

    protected[this] type M[-A, +B] = ConstantModel[B]

    private[this] class Parser extends BasicModelParser[M] {
        val modelType = "Constant"
        private val valueField = "value"

        def modelJsonReader[A, B: JsonReader : ScoreConverter, C >: M[A, B]] = new JsonReader[C] {
            def read(json: JsValue) = {
                val model = for {
                    jsV <- json(valueField)
                    mId <- getModelId(json)
                    v = jsV.convertTo[B]
                    m = new ConstantModel[B](ModelOutput(v), mId)
                } yield m

                model getOrElse { throw new DeserializationException("") }
            }
        }
    }

//    private[this] class Parser extends ModelParser[M] {
//        val modelType = "Constant"
//        private val valueField = "value"
//        def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]] = new JsonReader[C] {
//            def read(json: JsValue) = {
//                val model = for {
//                    jsV <- json(valueField)
//                    mId <- getModelId(json)
//                    v = jsV.convertTo[B]
//                    m = new ConstantModel[B](ModelOutput(v), mId)
//                } yield m
//
//                model getOrElse { throw new DeserializationException("") }
//            }
//        }
//    }

    val parser = new Parser().asInstanceOf[ModelParser[M]]
}
