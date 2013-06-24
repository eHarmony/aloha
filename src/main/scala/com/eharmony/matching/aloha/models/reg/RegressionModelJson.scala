package com.eharmony.matching.aloha.models.reg

import scala.collection.immutable.ListMap

import spray.json._
import spray.json.DefaultJsonProtocol._

import com.eharmony.matching.aloha.id.ModelId
import com.eharmony.matching.aloha.factory.ScalaJsonFormats.listMapFormat

trait RegressionModelJson {

    protected[this] final case class Spec(spec: String, defVal: Option[Seq[(String, Double)]] = Option(Nil))

    protected[this] final case class Hof(features: Map[String, Seq[String]], wt: Double)

    protected[this] final case class RegData(
        modelType: String,
        modelId: ModelId,
        notes: Option[Seq[String]],
        features: ListMap[String, Spec],
        weights: ListMap[String, Double],
        higherOrderFeatures: Option[Seq[Hof]],
        spline: Option[ConstantDeltaSpline],
        numMissingThreshold: Option[Int])

    protected[this] final val specJsonFormat = jsonFormat2(Spec)

    protected[this] implicit object FeatureSpecFormat extends JsonFormat[Spec] {
        def read(json: JsValue) = json match {
            case JsString(s) => Spec(s)
            case o : JsObject => o.convertTo[Spec](specJsonFormat)
            case e => throw new DeserializationException(s"unexpected feature $e")
        }

        def write(spec: Spec) = spec.defVal.map {
            case s if s.nonEmpty => JsObject(Map("spec" -> JsString(spec.spec), "defVal" -> s.toJson))
            case s if s.isEmpty => JsString(spec.spec)
        } getOrElse { JsString(spec.spec) }
    }

    protected[this] final implicit val modelIdFormat = jsonFormat2(ModelId.apply)
    protected[this] final implicit val hofJsonFormat = jsonFormat2(Hof)
    protected[this] final implicit val regSplineJsonFormat = jsonFormat(ConstantDeltaSpline, "min", "max", "knots")
    protected[this] final implicit val regDataJsonFormat = jsonFormat8(RegData)
}
