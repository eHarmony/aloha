package com.eharmony.matching.featureSpecExtractor.json

import scala.collection.{immutable => sci}

trait CovariateJson[Density] {
    val imports: sci.Seq[String]
    val features: sci.IndexedSeq[Spec[Density]]
}
