package com.eharmony.aloha.dataset.vw.multilabel.json

import com.eharmony.aloha.dataset.json.{Namespace, SparseSpec}
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 9/13/17.
  */
final case class VwMultilabeledJson(
    imports: sci.Seq[String],
    features: sci.IndexedSeq[SparseSpec],
    namespaces: Option[Seq[Namespace]] = Some(Nil),
    normalizeFeatures: Option[Boolean] = Some(false),
    positiveLabels: String)
  extends VwJsonLike

object VwMultilabeledJson extends DefaultJsonProtocol {
  implicit val labeledVwJsonFormat: RootJsonFormat[VwMultilabeledJson] =
    jsonFormat5(VwMultilabeledJson.apply)
}
