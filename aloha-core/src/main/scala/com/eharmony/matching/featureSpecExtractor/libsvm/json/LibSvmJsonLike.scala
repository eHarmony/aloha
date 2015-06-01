package com.eharmony.matching.featureSpecExtractor.libsvm.json

import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.json.CovariateJson
import com.eharmony.matching.featureSpecExtractor.json.validation.{FeatureValidation, Validation}

trait LibSvmJsonLike extends CovariateJson[Sparse]
with Validation
with FeatureValidation[Sparse] {
    def validate() = validateFeatureNames
}
