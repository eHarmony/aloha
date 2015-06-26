package com.eharmony.aloha.dataset.vw.cb

import com.eharmony.aloha.dataset.vw.VwCovariateProducer
import com.eharmony.aloha.dataset.vw.cb.json.VwContextualBanditJson
import com.eharmony.aloha.dataset.{CompilerFailureMessages, DvProducer, SparseCovariateProducer, SpecProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
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
