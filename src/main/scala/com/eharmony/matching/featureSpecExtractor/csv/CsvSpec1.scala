package com.eharmony.matching.featureSpecExtractor.csv

import com.eharmony.matching.featureSpecExtractor.{Spec, FeatureExtractorFunction}

final case class CsvSpec1[A](features: FeatureExtractorFunction[A, String], separator: String = ",")
    extends Spec[A] {
    def apply(data: A) = {
        val (missing, values) = features(data)
        (missing, values.mkString(separator))
    }
}
