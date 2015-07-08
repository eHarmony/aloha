package com.eharmony.aloha.dataset.libsvm.unlabeled

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.libsvm.unlabeled.json.LibSvmUnlabeledJson
import com.eharmony.aloha.dataset.{CompilerFailureMessages, FeatureExtractorFunction, SparseCovariateProducer, SpecProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.util.Logging
import com.eharmony.aloha.util.hashing.HashFunction
import spray.json.JsValue

import scala.util.Try


final case class LibSvmSpecProducer[A]()
extends SpecProducer[A, LibSvmSpec[A]]
   with SparseCovariateProducer
   with CompilerFailureMessages
   with Logging {

    type JsonType = LibSvmUnlabeledJson
    def name = getClass.getSimpleName
    def parse(json: JsValue): Try[LibSvmUnlabeledJson] = Try { json.convertTo[LibSvmUnlabeledJson] }
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: LibSvmUnlabeledJson): Try[LibSvmSpec[A]] = {
        val covariates: Try[FeatureExtractorFunction[A, Sparse]] = getCovariates(semantics, jsonSpec)
        val hash: HashFunction = jsonSpec.salt match {
            case Some(s) => new com.eharmony.aloha.util.hashing.MurmurHash3(s)
            case None    => com.eharmony.aloha.util.hashing.MurmurHash3
        }
        warn(hash.salts)
        covariates.map(c => jsonSpec.numBits.fold(new LibSvmSpec(c, hash))(b => new LibSvmSpec(c, hash, b)))
    }
}
