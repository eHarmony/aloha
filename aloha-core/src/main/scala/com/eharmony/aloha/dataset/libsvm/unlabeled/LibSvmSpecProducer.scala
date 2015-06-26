package com.eharmony.aloha.dataset.libsvm.unlabeled

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.libsvm.unlabeled.json.LibSvmUnlabeledJson
import com.eharmony.aloha.dataset.{CompilerFailureMessages, FeatureExtractorFunction, SparseCovariateProducer, SpecProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.google.common.hash.Hashing.murmur3_32
import spray.json.JsValue

import scala.util.Try


final case class LibSvmSpecProducer[A]()
extends SpecProducer[A, LibSvmSpec[A]]
   with SparseCovariateProducer
   with CompilerFailureMessages {

    type JsonType = LibSvmUnlabeledJson
    def name = getClass.getSimpleName
    def parse(json: JsValue): Try[LibSvmUnlabeledJson] = Try { json.convertTo[LibSvmUnlabeledJson] }
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: LibSvmUnlabeledJson): Try[LibSvmSpec[A]] = {
        val covariates: Try[FeatureExtractorFunction[A, Sparse]] = getCovariates(semantics, jsonSpec)
        val salt = jsonSpec.salt.getOrElse(LibSvmSpecProducer.Salt)
        // TODO: Log seed on warn level.
        covariates.map(c => jsonSpec.numBits.fold(new LibSvmSpec(c, murmur3_32(salt)))(b => new LibSvmSpec(c, murmur3_32(salt), b)))
    }
}

object LibSvmSpecProducer {
    private[libsvm] val Salt = 0
}
