package com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.json

import com.eharmony.matching.featureSpecExtractor.json.SparseSpec
import com.eharmony.matching.featureSpecExtractor.libsvm.json.LibSvmJsonLike
import spray.json._

import scala.collection.{immutable => sci}

final case class LibSvmUnlabeledJson(
        imports: sci.Seq[String],
        features: sci.IndexedSeq[SparseSpec],
        numBits: Option[Int],
        salt: Option[Int])
extends LibSvmJsonLike

object LibSvmUnlabeledJson extends DefaultJsonProtocol {
    implicit val libSvmUnlabeledFormat = jsonFormat4(LibSvmUnlabeledJson.apply)
}
