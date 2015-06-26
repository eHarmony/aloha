package com.eharmony.aloha.dataset.libsvm.json

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.json.CovariateJson
import com.eharmony.aloha.dataset.json.validation.{FeatureValidation, Validation}

trait LibSvmJsonLike extends CovariateJson[Sparse]
with Validation
with FeatureValidation[Sparse] {
    def validate() = validateFeatureNames
}
