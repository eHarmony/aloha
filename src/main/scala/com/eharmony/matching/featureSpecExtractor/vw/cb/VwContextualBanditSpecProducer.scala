package com.eharmony.matching.featureSpecExtractor.vw.cb

import scala.util.Try

import spray.json.{DefaultJsonProtocol, JsValue}

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.vw.VwCovariateProducer
import com.eharmony.matching.featureSpecExtractor.vw.cb.json.VwContextualBanditJson
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, DvProducer, SparseCovariateProducer, SpecProducer}


final class VwContextualBanditSpecProducer[A]
extends SpecProducer[A, VwContextualBanditSpec[A]]
   with VwCovariateProducer[A]
   with DvProducer
   with DefaultJsonProtocol
   with SparseCovariateProducer
   with CompilerFailureMessages {


    type JsonType = VwContextualBanditJson
    private[this] implicit val labeledVwJsonFormat = jsonFormat7(VwContextualBanditJson)

    def name = getClass.getSimpleName

    def parse(json: JsValue): Try[VwContextualBanditJson] = Try { json.convertTo[VwContextualBanditJson] }


    def getSpec(semantics: CompiledSemantics[A], jsonSpec: VwContextualBanditJson): Try[VwContextualBanditSpec[A]] = {
        val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

        val spec = for {
            cov <- covariates
            sem = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
            action <- getAction(sem, jsonSpec.cbAction)
            cost <- getCost(sem, jsonSpec.cbCost)
            prob <- getProbability(sem, jsonSpec.cbCost)
        } yield new VwContextualBanditSpec(cov, default, nss, normalizer, action, cost, prob)

        spec
    }

    protected[this] def getAction(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, String]] =
        getDv(semantics, "cbAction", Some(spec), Some(""))

    protected[this] def getCost(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, String]] =
        getDv(semantics, "cbCost", Some(spec), Some(""))

    protected[this] def getProbability(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, String]] =
        getDv(semantics, "cbProbability", Some(spec), Some(""))
}
