package com.eharmony.aloha.dataset.libsvm.labeled.json

import com.eharmony.aloha.dataset.json.SparseSpec
import com.eharmony.aloha.dataset.libsvm.json.LibSvmJsonLike
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
