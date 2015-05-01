package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.vw.VwCovariateProducer
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.json.UnlabeledVwJson
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, SparseCovariateProducer, SpecProducer}
import spray.json._

import scala.util.Try


class VwSpecProducer[A]
extends SpecProducer[A, VwSpec[A]]
   with VwCovariateProducer[A]
   with DefaultJsonProtocol
   with SparseCovariateProducer
   with CompilerFailureMessages {

    type JsonType = UnlabeledVwJson
    private[this] implicit val unlabeledVwJsonFormat = jsonFormat4(UnlabeledVwJson)
    def specProducerName = getClass.getSimpleName
    def parse(json: JsValue): Try[UnlabeledVwJson] = Try { json.convertTo[UnlabeledVwJson] }
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: UnlabeledVwJson): Try[VwSpec[A]] = {
        val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)
        val spec = covariates.map(c => new VwSpec(c, default, nss, normalizer))
        spec
    }
}
