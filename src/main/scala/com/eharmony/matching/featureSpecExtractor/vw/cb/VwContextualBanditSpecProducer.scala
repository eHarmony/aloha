package com.eharmony.matching.featureSpecExtractor.vw.cb

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.vw.VwCovariateProducer
import com.eharmony.matching.featureSpecExtractor.vw.cb.json.VwContextualBanditJson
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, DvProducer, SparseCovariateProducer, SpecProducer}
import spray.json.JsValue

import scala.util.Try


final class VwContextualBanditSpecProducer[A]
extends SpecProducer[A, VwContextualBanditSpec[A]]
   with VwCovariateProducer[A]
   with DvProducer
   with SparseCovariateProducer
   with CompilerFailureMessages {


    type JsonType = VwContextualBanditJson

    def name = getClass.getSimpleName

    def parse(json: JsValue): Try[VwContextualBanditJson] = Try { json.convertTo[VwContextualBanditJson] }

    def getSpec(semantics: CompiledSemantics[A], jsonSpec: VwContextualBanditJson): Try[VwContextualBanditSpec[A]] = {
        val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

        val spec = for {
            cov <- covariates
            action <- getAction(semantics, jsonSpec.cbAction)
            cost <- getCost(semantics, jsonSpec.cbCost)
            prob <- getProbability(semantics, jsonSpec.cbProbability)
        } yield new VwContextualBanditSpec(cov, default, nss, normalizer, action, cost, prob)

        spec
    }


    protected[this] def getAction(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, Option[Long]]] =
        getDv[A, Option[Long]](semantics, "cbAction", Some(s"Option($spec)"), Some(None))

    protected[this] def getCost(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, Option[Double]]] =
        getDv[A, Option[Double]](semantics, "cbCost", Some(s"Option($spec)"), Some(None))

    protected[this] def getProbability(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, Option[Double]]] =
        getDv[A, Option[Double]](semantics, "cbProbability", Some(s"Option($spec)"), Some(None))
}
