package com.eharmony.aloha.factory

import scala.language.higherKinds


import spray.json._
import spray.json.DefaultJsonProtocol.{LongJsonFormat, StringJsonFormat}

import com.eharmony.aloha.id.{ModelIdentity, ModelId}
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.factory.pimpz.JsValuePimpz
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.factory.ex.AlohaFactoryException

/**
  * '''NOTE''': If the covariance annotation on M were removed we could change the signature of modelJsonReader from
  * {{{ def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]]: JsonReader[C] }}}
  * to
  * {{{ def modelJsonReader[A, B: JsonReader: ScoreConverter]: JsonReader[M[A, B]] }}}
  */
trait ModelParser extends JsValuePimpz {
    private implicit val modelIdFormat = DefaultJsonProtocol.jsonFormat2(ModelId.apply)

    val modelType: String

    protected[this] final def jsonReaderToJsonFormat[A](implicit jr: JsonReader[A]): JsonFormat[A] = jr match {
        case jf: JsonFormat[A] => jf
        case _ => new JsonFormat[A] {
            def write(a: A): JsValue = throw new UnsupportedOperationException("write not supported")
            def read(json: JsValue): A = jr.read(json)
        }
    }

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
