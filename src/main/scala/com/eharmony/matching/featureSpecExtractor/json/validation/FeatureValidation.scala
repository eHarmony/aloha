package com.eharmony.matching.featureSpecExtractor.json.validation

import com.eharmony.matching.featureSpecExtractor.json.Spec

import scala.collection.{immutable => sci}

trait FeatureValidation[Density] extends ValidationBase {
    val features: sci.IndexedSeq[Spec[Density]]

    protected[this] final def validateFeatureNames: Option[String] =
        reportDuplicates("duplicate feature names detected", features.view)(_.name)
}
