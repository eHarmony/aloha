package com.eharmony.aloha.dataset.libsvm.labeled

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmSpec
import com.eharmony.aloha.dataset.{FeatureExtractorFunction, LabelSpec}
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.hashing.HashFunction

// import com.google.common.hash.HashFunction

class LibSvmLabelSpec[-A](
        covariates: FeatureExtractorFunction[A, Sparse],
        label: GenAggFunc[A, String],
        hash: HashFunction,
        numBits: Int = LibSvmSpec.DefaultBits)
extends LibSvmSpec[A](covariates, hash, numBits)
   with LabelSpec[A] {

    override def apply(data: A) = {
        val (missing, iv) = super.apply(data)
        val lab = label(data)
        val sb = new StringBuilder().append(lab).append(" ").append(iv)
        (missing, sb)
    }

    override def stringLabel = label.andThenGenAggFunc(Option.apply)
}
