package com.eharmony.aloha.models.reg.json

import com.eharmony.aloha.factory.ScalaJsonFormats.listMapFormat
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.reg.ConstantDeltaSpline
import spray.json.DefaultJsonProtocol._

import scala.collection.immutable.ListMap

trait RegressionModelJson extends SpecJson {

  protected[this] case class Hof(features: Map[String, Seq[String]], wt: Double) extends Serializable

  protected[this] case class RegData(
    modelType: String,
    modelId: ModelId,
    notes: Option[Seq[String]],
    features: ListMap[String, Spec],
    weights: ListMap[String, Double],
    higherOrderFeatures: Option[Seq[Hof]],
    spline: Option[ConstantDeltaSpline],
    numMissingThreshold: Option[Int])


  protected[this] final implicit val hofJsonFormat = jsonFormat2(Hof)
  protected[this] final implicit val regSplineJsonFormat = jsonFormat(ConstantDeltaSpline, "min", "max", "knots")
  protected[this] final implicit val regDataJsonFormat = jsonFormat8(RegData)
}
