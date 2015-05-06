package com.eharmony.matching.featureSpecExtractor.libsvm.labeled

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.FeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.LibSvmSpec
import com.google.common.hash.HashFunction

class LibSvmLabelSpec[A](
        covariates: FeatureExtractorFunction[A, Sparse],
        label: GenAggFunc[A, String],
        hash: HashFunction,
        numBits: Int = LibSvmSpec.DefaultBits)
extends LibSvmSpec[A](covariates, hash, numBits) {
    override def toInput(data: A, includeZeroValues: Boolean, sb: StringBuilder) = {
        val lab = label(data)
        val b = sb.append(lab).append(" ")
        super.toInput(data, includeZeroValues, b)
    }
}
