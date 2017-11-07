package com.eharmony.aloha.dataset.vw.multilabel.json

import com.eharmony.aloha.dataset.json.{Namespace, SparseSpec}
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 11/6/17.
  *
  * @param imports
  * @param features
  * @param namespaces
  * @param normalizeFeatures
  * @param positiveLabels
  * @param numDownsampledNegLabels
  */
final case class VwDownsampledMultilabeledJson(
    imports: sci.Seq[String],
    features: sci.IndexedSeq[SparseSpec],
    namespaces: Option[Seq[Namespace]] = Some(Nil),
    normalizeFeatures: Option[Boolean] = Some(false),
    positiveLabels: String,
    numDownsampledNegLabels: Short
) extends VwJsonLike {

  require(
    0 < numDownsampledNegLabels,
    s"numDownsampledNegLabels must be positive, found $numDownsampledNegLabels"
  )
}

object VwDownsampledMultilabeledJson extends DefaultJsonProtocol {
  implicit val vwDownsampledMultilabeledJson: RootJsonFormat[VwDownsampledMultilabeledJson] =
    jsonFormat6(VwDownsampledMultilabeledJson.apply)
}
