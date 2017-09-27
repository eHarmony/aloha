package com.eharmony.aloha.models.multilabel.json

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.multilabel.PluginInfo
import com.eharmony.aloha.models.reg.json.{Spec, SpecJson}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsonFormat, RootJsonFormat}

import scala.collection.immutable.ListMap
import com.eharmony.aloha.factory.ScalaJsonFormats

trait MultilabelModelJson extends SpecJson with ScalaJsonFormats {

  protected[this] case class Plugin(`type`: String)

  /**
    * Data for the
    *
    * @param modelType
    * @param modelId
    * @param features
    * @param numMissingThreshold
    * @param labelsInTrainingSet
    * @param labelsOfInterest
    * @param underlying the underlying model that will be produced by a
    * @tparam K
    */
  protected[this] case class MultilabelData[K](
      modelType: String,
      modelId: ModelId,
      features: ListMap[String, Spec],
      numMissingThreshold: Option[Int],
      labelsInTrainingSet: Vector[K],
      labelsOfInterest: Option[String],
      underlying: JsObject
  ) extends PluginInfo[K]

  protected[this] final implicit def multilabelDataJsonFormat[K: JsonFormat]: RootJsonFormat[MultilabelData[K]] =
    jsonFormat7(MultilabelData[K])

  protected[this] final implicit val pluginJsonFormat: RootJsonFormat[Plugin] =
    jsonFormat1(Plugin)
}
