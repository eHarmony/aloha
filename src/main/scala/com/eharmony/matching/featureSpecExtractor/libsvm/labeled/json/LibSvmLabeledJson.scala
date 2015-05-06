package com.eharmony.matching.featureSpecExtractor.libsvm.labeled.json

import com.eharmony.matching.featureSpecExtractor.json.SparseSpec
import com.eharmony.matching.featureSpecExtractor.libsvm.json.LibSvmJsonLike
import spray.json._

import scala.collection.{immutable => sci}

case class LibSvmLabeledJson(
        imports: Seq[String],
        features: sci.IndexedSeq[SparseSpec],
        numBits: Option[Int],
        label: String)
extends LibSvmJsonLike

object LibSvmLabeledJson extends DefaultJsonProtocol {
    implicit val libSvmLabeledFormat = jsonFormat4(LibSvmLabeledJson.apply)
}
