package com.eharmony.matching.aloha.factory

import scala.language.higherKinds


import spray.json.{JsObject, JsValue, JsonReader, DefaultJsonProtocol}
import spray.json.DefaultJsonProtocol.{LongJsonFormat, StringJsonFormat}

import com.eharmony.matching.aloha.id.{ModelIdentity, ModelId}
import com.eharmony.matching.aloha.models.Model
import com.eharmony.matching.aloha.factory.pimpz.JsValuePimpz
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.factory.ex.AlohaFactoryException

/**
  * '''NOTE''': If the covariance annotation on M were removed we could change the signature of modelJsonReader from
  * {{{ def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]]: JsonReader[C] }}}
  * to
  * {{{ def modelJsonReader[A, B: JsonReader: ScoreConverter]: JsonReader[M[A, B]] }}}
  */
trait ModelParser extends JsValuePimpz {
    private implicit val modelIdFormat = DefaultJsonProtocol.jsonFormat2(ModelId.apply)

    val modelType: String

    /**
      * @param factory ModelFactory[Model[_, _] ]
      * @tparam A model input type
      * @tparam B model input type
      * @return
      */
    def modelJsonReader[A, B: JsonReader: ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]]): JsonReader[_ <: Model[A, B]]

    protected final def getModelId(json: JsValue): Option[ModelIdentity] =
        json(ModelParser.modelIdField).collect{case o: JsObject => o.convertTo[ModelId]}

    final def getParser[A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]]) = new Parser[JsValue, Model[A, B]] {
        def parse(json: JsValue): Model[A, B] = json.convertTo(modelJsonReader[A, B](factory, semantics))
    }
}

trait BasicModelParser extends ModelParser {
    def modelJsonReader[A, B: JsonReader: ScoreConverter]: JsonReader[_ <: Model[A, B]]
    final def modelJsonReader[A, B: JsonReader: ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]]): JsonReader[_ <: Model[A, B]] =
        modelJsonReader[A, B]
}

trait ModelParserWithSemantics extends ModelParser {
    def modelJsonReader[A, B: JsonReader: ScoreConverter](semantics: Semantics[A]): JsonReader[_ <: Model[A, B]]
    final def modelJsonReader[A, B: JsonReader: ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]]): JsonReader[_ <: Model[A, B]] = {
        semantics.map(s => modelJsonReader[A, B](s)).getOrElse { throw new AlohaFactoryException(this.toString + "(ModelParserWithSemantics): No semantics provided") }
    }
}

private object ModelParser {
    val modelIdField = "modelId"
}
