package com.eharmony.matching.featureSpecExtractor.vw.cb

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.json.JsonSpec
import com.eharmony.matching.featureSpecExtractor.vw.Vw
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpecProducer
import com.eharmony.matching.featureSpecExtractor.{DvProducer, Spec}

import scala.util.Try

class VwContextualBanditSpecProducer extends VwSpecProducer with DvProducer {
    override def specProducerName = getClass.getSimpleName
    override def appliesTo(jsonSpec: JsonSpec): Boolean = jsonSpec.specType.exists(_ == Vw.identifier) && jsonSpec.cbAction.isDefined
    override def getSpec[A](semantics: CompiledSemantics[A], jsonSpec: JsonSpec): Try[Spec[A]] = {
        val (covariates, default, nss, normalizer) = getInputs(semantics, jsonSpec)

        val spec = for {
            cov <- covariates
            sem = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
            action <- getAction(sem, jsonSpec.cbAction)
            cost <- getCost(sem, jsonSpec.cbCost)
            prob <- getProbability(sem, jsonSpec.cbCost)
        } yield new VwContextualBanditSpec(cov, default, nss, normalizer, action, cost, prob)

        spec
    }


    protected[this] def getAction[A](semantics: CompiledSemantics[A], spec: Option[String]): Try[GenAggFunc[A, String]] =
        getDv(semantics, "cbAction", spec, Some(""))

    protected[this] def getCost[A](semantics: CompiledSemantics[A], spec: Option[String]): Try[GenAggFunc[A, String]] =
        getDv(semantics, "cbCost", spec, Some(""))

    protected[this] def getProbability[A](semantics: CompiledSemantics[A], spec: Option[String]): Try[GenAggFunc[A, String]] =
        getDv(semantics, "cbProbability", spec, Some(""))

    protected[this] def getImportance[A](semantics: CompiledSemantics[A], spec: Option[String]): Option[GenAggFunc[A, String]] = {
        getDv(semantics, "importance", spec, Some("")).map(Option.apply).getOrElse(None)
    }}
