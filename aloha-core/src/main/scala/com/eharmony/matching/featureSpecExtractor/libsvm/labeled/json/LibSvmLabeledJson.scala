package com.eharmony.matching.featureSpecExtractor.libsvm.labeled.json

import com.eharmony.matching.featureSpecExtractor.json.SparseSpec
import com.eharmony.matching.featureSpecExtractor.libsvm.json.LibSvmJsonLike
import spray.json._

import scala.collection.{immutable => sci}

final case class LibSvmLabeledJson(
        imports: sci.Seq[String],
        features: sci.IndexedSeq[SparseSpec],
        numBits: Option[Int],
        salt: Option[Int],
        label: String)
extends LibSvmJsonLike

object LibSvmLabeledJson extends DefaultJsonProtocol {
    implicit val libSvmLabeledFormat = jsonFormat5(LibSvmLabeledJson.apply)
}
