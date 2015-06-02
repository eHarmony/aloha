package com.eharmony.matching.aloha.models.vw.jni

import spray.json.RootJsonFormat

import scala.collection.immutable.ListMap
import spray.json.DefaultJsonProtocol._

import com.eharmony.matching.aloha.id.ModelId
import com.eharmony.matching.aloha.models.reg.json.SpecJson
import com.eharmony.matching.aloha.factory.Formats.listMapFormat

trait VwJNIModelJson extends SpecJson {
    protected[this] final case class Vw(params: Either[Seq[String], String])

    protected[this] final case class VwJNIAst(
        modelType: String,
        modelId: ModelId,
        features: ListMap[String, Spec],
        namespaces: ListMap[String, Seq[String]],
        vw: Vw,
        numMissingThreshold: Option[Int])

    protected[this] final implicit val vwFormat: RootJsonFormat[Vw] = jsonFormat1(Vw.apply)
    protected[this] final implicit val vwJNIAstFormat: RootJsonFormat[VwJNIAst] = jsonFormat6(VwJNIAst.apply)
}
