package com.eharmony.aloha.models.vw.jni.multilabel.json

import com.eharmony.aloha.io.sources.ModelSource

import spray.json.DefaultJsonProtocol._

import scala.collection.immutable.ListMap
import com.eharmony.aloha.factory.ScalaJsonFormats

/**
  * Created by ryan.deak on 9/8/17.
  */
trait VwMultilabelModelJson extends ScalaJsonFormats {

  private[multilabel] case class VwMultilabelAst(
      `type`: String,
      modelSource: ModelSource,
      params: Either[Seq[String], String] = Right(""),
      namespaces: Option[ListMap[String, Seq[String]]] = Some(ListMap.empty)
  )

  protected[this] implicit val vwMultilabelAstFormat = jsonFormat4(VwMultilabelAst)
}
