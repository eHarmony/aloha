package com.eharmony.aloha.dataset.vw.unlabeled.json

import com.eharmony.aloha.dataset.json.{Namespace, SparseSpec}
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import spray.json.DefaultJsonProtocol

import scala.collection.{immutable => sci}

final case class VwUnlabeledJson(
        imports: sci.Seq[String],
        features: sci.IndexedSeq[SparseSpec],
        namespaces: Option[Seq[Namespace]] = None,
        normalizeFeatures: Option[Boolean] = Some(false))
extends VwJsonLike

object VwUnlabeledJson extends DefaultJsonProtocol {
    implicit val unlabeledVwJsonFormat = jsonFormat4(VwUnlabeledJson.apply)
}
