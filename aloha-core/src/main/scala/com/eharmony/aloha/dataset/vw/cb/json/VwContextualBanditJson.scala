package com.eharmony.aloha.dataset.vw.cb.json

import com.eharmony.aloha.dataset.json.{Namespace, SparseSpec}
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import com.eharmony.aloha.util.SimpleTypeSeq
import spray.json._

import scala.collection.{immutable => sci}

final case class VwContextualBanditJson(
    imports: sci.Seq[String],
    features: sci.IndexedSeq[SparseSpec],
    namespaces: Option[Seq[Namespace]] = Some(Nil),
    normalizeFeatures: Option[Boolean] = Some(false),
    cbAction: String,
    cbCost: String,
    cbProbability: String,
    classLabels: Option[SimpleTypeSeq])
extends VwJsonLike

object VwContextualBanditJson extends DefaultJsonProtocol {
  implicit val labeledVwJsonFormat = jsonFormat8(VwContextualBanditJson.apply)
}
