package com.eharmony.aloha.dataset.vw.unlabeled

import com.eharmony.aloha.dataset.vw.VwCovariateProducer
import com.eharmony.aloha.dataset.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.aloha.dataset.{CompilerFailureMessages, SparseCovariateProducer, SpecProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
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
