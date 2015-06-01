package com.eharmony.matching.featureSpecExtractor.vw.labeled.json

import com.eharmony.matching.featureSpecExtractor.vw.json.VwJsonLike
import spray.json.DefaultJsonProtocol

import scala.collection.{immutable => sci}
import scala.util.Try

import com.eharmony.matching.featureSpecExtractor.json.{Namespace, SparseSpec}


final case class VwLabeledJson(
        imports: sci.Seq[String],
        features: sci.IndexedSeq[SparseSpec],
        namespaces: Option[Seq[Namespace]] = Some(Nil),
        normalizeFeatures: Option[Boolean] = Some(false),
        label: String,
        importance: Option[String] = Some("1"),
        tag: Option[String] = None)
extends VwJsonLike {

    def validateImportance(): Boolean = {
        importance.nonEmpty || Try {
            importance.get.trim.toDouble
        }.map {
            case d if d >= 0 => true
            case _ => false
        }.getOrElse(true)
    }
}

object VwLabeledJson extends DefaultJsonProtocol {
    implicit val labeledVwJsonFormat = jsonFormat7(VwLabeledJson.apply)
}
