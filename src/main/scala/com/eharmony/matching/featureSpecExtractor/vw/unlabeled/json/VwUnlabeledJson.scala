package com.eharmony.matching.featureSpecExtractor.vw.unlabeled.json

import com.eharmony.matching.featureSpecExtractor.json.{Namespace, SparseSpec}
import com.eharmony.matching.featureSpecExtractor.vw.json.VwJsonLike
import spray.json.DefaultJsonProtocol

import scala.collection.{immutable => sci}

case class VwUnlabeledJson(
        imports: Seq[String],
        features: sci.IndexedSeq[SparseSpec],
        namespaces: Option[Seq[Namespace]] = None,
        normalizeFeatures: Option[Boolean] = Some(false))
extends VwJsonLike

object VwUnlabeledJson extends DefaultJsonProtocol {
    implicit val unlabeledVwJsonFormat = jsonFormat4(VwUnlabeledJson.apply)
}
