package com.eharmony.aloha.models.reg.json

import spray.json._
import spray.json.DefaultJsonProtocol._


case class Spec(spec: String, defVal: Option[Seq[(String, Double)]] = Option(Nil))

trait SpecJson {

    protected[this] final val specJsonFormat = jsonFormat2(Spec)

    protected[this] implicit object FeatureSpecFormat extends JsonFormat[Spec] {
        def read(json: JsValue) = json match {
            case JsString(s) => Spec(s)
            case o: JsObject => o.convertTo[Spec](specJsonFormat)
            case e => throw new DeserializationException(s"unexpected feature $e")
        }

        def write(spec: Spec) = spec.defVal.map {
            case s if s.nonEmpty => JsObject(Map("spec" -> JsString(spec.spec), "defVal" -> s.toJson))
            case s if s.isEmpty => JsString(spec.spec)
        } getOrElse { JsString(spec.spec) }
    }
}
