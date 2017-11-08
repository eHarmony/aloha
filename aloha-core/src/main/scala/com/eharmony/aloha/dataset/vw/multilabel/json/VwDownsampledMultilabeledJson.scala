package com.eharmony.aloha.dataset.vw.multilabel.json

import com.eharmony.aloha.dataset.json.{Namespace, SparseSpec}
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.collection.{immutable => sci}

/**
  * JSON AST for `VwDownsampledMultilabelRowCreator`.
  * @param imports
  * @param features
  * @param namespaces
  * @param normalizeFeatures
  * @param positiveLabels string representing a function that will be used to extract positive
  *                       labels from the input.
  * @param numDownsampledNegLabels '''a positive value''' representing the number of negative
  *                                labels to include in each row.  If this is less than the
  *                                number of negative examples for a given row, then no
  *                                downsampling of negatives will take place.
  * @author deaktator
  * @since 11/6/2017
  */
final case class VwDownsampledMultilabeledJson(
    imports: sci.Seq[String],
    features: sci.IndexedSeq[SparseSpec],
    namespaces: Option[Seq[Namespace]] = Some(Nil),
    normalizeFeatures: Option[Boolean] = Some(false),
    positiveLabels: String,
    numDownsampledNegLabels: Int
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
