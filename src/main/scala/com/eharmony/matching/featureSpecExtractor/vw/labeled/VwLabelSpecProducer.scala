package com.eharmony.matching.featureSpecExtractor.vw.labeled

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.vw.VwCovariateProducer
import com.eharmony.matching.featureSpecExtractor.vw.labeled.json.VwLabeledJson
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, DvProducer, SparseCovariateProducer, SpecProducer}
import spray.json.{DefaultJsonProtocol, JsValue}

import scala.util.Try

final class VwLabelSpecProducer[A]
extends SpecProducer[A, VwLabelSpec[A]]
   with VwCovariateProducer[A]
   with DvProducer
   with DefaultJsonProtocol
   with SparseCovariateProducer
   with CompilerFailureMessages {

    type JsonType = VwLabeledJson
    private[this] implicit val labeledVwJsonFormat = jsonFormat6(VwLabeledJson)

    def specProducerName = getClass.getSimpleName

    def parse(json: JsValue): Try[VwLabeledJson] = Try { json.convertTo[VwLabeledJson] }

    def getSpec(semantics: CompiledSemantics[A], jsonSpec: VwLabeledJson): Try[VwLabelSpec[A]] = {
        val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

        val spec = for {
            cov <- covariates
            sem = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
            lab <- getLabel(sem, jsonSpec.label)
            importance = getImportance(semantics, jsonSpec.importance)
        } yield new VwLabelSpec(cov, default, nss, normalizer, lab, importance)

        spec
    }

    protected[this] def getLabel(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, String]] =
        getDv(semantics, "label", Some(spec), Some(""))

    protected[this] def getImportance(semantics: CompiledSemantics[A], spec: Option[String]): Option[GenAggFunc[A, String]] =
        getDv(semantics, "importance", spec, Some("")).map(Option.apply).getOrElse(None)
}
