package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.vw.VwCovariateProducer
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, SparseCovariateProducer, SpecProducer}
import spray.json._

import scala.util.Try


final class VwSpecProducer[A]
extends SpecProducer[A, VwSpec[A]]
   with VwCovariateProducer[A]
   with SparseCovariateProducer
   with CompilerFailureMessages {

    type JsonType = VwUnlabeledJson
    def name = getClass.getSimpleName
    def parse(json: JsValue): Try[VwUnlabeledJson] = Try { json.convertTo[VwUnlabeledJson] }
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: VwUnlabeledJson): Try[VwSpec[A]] = {
        val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)
        val spec = covariates.map(c => new VwSpec(c, default, nss, normalizer))
        spec
    }
}
