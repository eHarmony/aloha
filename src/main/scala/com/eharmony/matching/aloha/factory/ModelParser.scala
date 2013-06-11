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

/**
  * '''NOTE''': If the covariance annotation on M were removed we could change the signature of modelJsonReader from
  * {{{ def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]]: JsonReader[C] }}}
  * to
  * {{{ def modelJsonReader[A, B: JsonReader: ScoreConverter]: JsonReader[M[A, B]] }}}
  * @tparam M The implementation of model that will be generated.  Notice that this is a two param
  */
trait ModelParser[+M[-_, +_] <: Model[_, _]] extends JsValuePimpz {
    private implicit val modelIdFormat = DefaultJsonProtocol.jsonFormat2(ModelId.apply)

    val modelType: String

    /**
      * @param factory ModelFactory[Model[_, _] ]
      * @tparam A
      * @tparam B
      * @tparam C
      * @return
      */
    def modelJsonReader[M1[-_, +_] <: Model[_,_], A, B: JsonReader: ScoreConverter, C >: M[A, B]](factory: Option[ModelFactory[M1]], semantics: Option[Semantics[A]]): JsonReader[C]

    protected final def getModelId(json: JsValue): Option[ModelIdentity] =
        json(ModelParser.modelIdField).collect{case o: JsObject => o.convertTo[ModelId]}

    final def getParser[M1[-_, +_] <: Model[_,_], A: RefInfo, B: RefInfo: JsonReader: ScoreConverter](factory: Option[ModelFactory[M1]], semantics: Option[Semantics[A]]) = new Parser[JsValue, M[A, B]] {
        def parse(json: JsValue): M[A, B] = json.convertTo(modelJsonReader[M1, A, B, M[A, B]](factory, semantics))
    }
}

trait BasicModelParser[+M[-_, +_] <: Model[_, _]] extends ModelParser[M] {
    def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]]: JsonReader[C]
    final def modelJsonReader[M1[-_, +_] <: Model[_,_], A, B: JsonReader: ScoreConverter, C >: M[A, B]](factory: Option[ModelFactory[M1]], semantics: Option[Semantics[A]]): JsonReader[C] =
        modelJsonReader[A, B, C]
}

trait ModelParserWithSemantics[+M[-_, +_] <: Model[_, _]] extends ModelParser[M] {
    def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]](semantics: Option[Semantics[A]]): JsonReader[C]
    final def modelJsonReader[M1[-_, +_] <: Model[_,_], A, B: JsonReader: ScoreConverter, C >: M[A, B]](factory: Option[ModelFactory[M1]], semantics: Option[Semantics[A]]): JsonReader[C] =
        modelJsonReader[A, B, C](semantics)
}


//trait ModelParser[+M[-_, +_] <: Model[_, _]] extends JsValuePimpz {
//    private implicit val modelIdFormat = DefaultJsonProtocol.jsonFormat2(ModelId.apply)
//
//    val modelType: String
//
//    /**
//     * @param factory ModelFactory[Model[_, _] ]
//     * @tparam A
//     * @tparam B
//     * @tparam C
//     * @return
//     */
//    def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]]: JsonReader[C]
//
//    protected final def getModelId(json: JsValue): Option[ModelIdentity] =
//        json(ModelParser.modelIdField).collect{case o: JsObject => o.convertTo[ModelId]}
//
//    final def getParser[A: TypeTag, B: TypeTag: JsonReader: ScoreConverter] = new Parser[JsValue, M[A, B]] {
//        def parse(json: JsValue): M[A, B] = json.convertTo(modelJsonReader[A, B, M[A, B]])
//    }
//}

private object ModelParser {
    val modelIdField = "modelId"
}
