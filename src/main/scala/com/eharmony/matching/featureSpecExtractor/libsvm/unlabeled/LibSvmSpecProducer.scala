package com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.json.LibSvmUnlabeledJson
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, FeatureExtractorFunction, SparseCovariateProducer, SpecProducer}
import com.google.common.hash.Hashing.murmur3_32
import spray.json.JsValue

import scala.util.Try


class LibSvmSpecProducer[A](seed: Int)
extends SpecProducer[A, LibSvmSpec[A]]
   with SparseCovariateProducer
   with CompilerFailureMessages {

    def this() = this(LibSvmSpecProducer.Seed)

    type JsonType = LibSvmUnlabeledJson
    def name = getClass.getSimpleName
    def parse(json: JsValue): Try[LibSvmUnlabeledJson] = Try { json.convertTo[LibSvmUnlabeledJson] }
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: LibSvmUnlabeledJson): Try[LibSvmSpec[A]] = {
        val covariates: Try[FeatureExtractorFunction[A, Sparse]] = getCovariates(semantics, jsonSpec)
        // TODO: Log seed on warn level.
        covariates.map(c => jsonSpec.numBits.fold(new LibSvmSpec(c, murmur3_32(seed)))(b => new LibSvmSpec(c, murmur3_32(seed), b)))
    }
}

object LibSvmSpecProducer {
    private val Seed = 0
}
