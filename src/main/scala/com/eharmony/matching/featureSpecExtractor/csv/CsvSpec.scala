package com.eharmony.matching.featureSpecExtractor.csv

import com.eharmony.matching.featureSpecExtractor.density.Dense
import com.eharmony.matching.featureSpecExtractor.{FeatureExtractorFunction, Spec}

final case class CsvSpec[A](features: FeatureExtractorFunction[A, Dense], separator: String = ",")
extends Spec[A] {
    def apply(data: A) = {
        val (missing, values) = features(data)
        (missing, values.mkString(separator))
    }
}
