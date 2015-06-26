package com.eharmony.matching.featureSpecExtractor.vw.labeled

import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc0}
import com.eharmony.matching.featureSpecExtractor.vw.VwCovariateProducer
import com.eharmony.matching.featureSpecExtractor.vw.labeled.json.VwLabeledJson
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, DvProducer, SparseCovariateProducer, SpecProducer}
import spray.json.JsValue

import scala.util.Try

final class VwLabelSpecProducer[A]
extends SpecProducer[A, VwLabelSpec[A]]
   with VwCovariateProducer[A]
   with DvProducer
   with SparseCovariateProducer
   with CompilerFailureMessages {

    type JsonType = VwLabeledJson

    def name = getClass.getSimpleName

    def parse(json: JsValue): Try[VwLabeledJson] = Try { json.convertTo[VwLabeledJson] }

    def getSpec(semantics: CompiledSemantics[A], jsonSpec: VwLabeledJson): Try[VwLabelSpec[A]] = {
        val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

        val importance = getImportance(semantics, jsonSpec.importance)

        // Notice that we create a new semantics for the tag because it has a string output type.  This new
        // semantics implicitly coerces values of any type to string by calling toString since all objects
        // have a toString method.  This allows more sloppy specifications but makes them work.
        val spec = for {
            cov <- covariates
            lab <- getLabel(semantics, jsonSpec.label)
            sem = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
            tag <- getTag(sem, jsonSpec.tag, lab)
        } yield new VwLabelSpec(cov, default, nss, normalizer, lab, importance, tag)

        spec
    }

    protected[this] def getLabel(semantics: CompiledSemantics[A], spec: String): Try[GenAggFunc[A, Option[Double]]] =
        getDv[A, Option[Double]](semantics, "label", Some(s"Option($spec)"), Some(None))

    protected[this] def getImportance(semantics: CompiledSemantics[A], spec: Option[String]): GenAggFunc[A, Option[Double]] =
        getDv[A, Option[Double]](semantics, "importance", spec.map(s => s"Option($s)"), Some(None)) getOrElse {
            GenFunc0[A, Option[Double]]("1", _ => Some(1d))
        }

    protected[this] def getTag(semantics: CompiledSemantics[A], spec: Option[String], label: GenAggFunc[A, Option[Double]]): Try[GenAggFunc[A, Option[String]]] = {
        spec.fold(Try{label.andThenGenAggFunc(_.map(v => VwSpec.LabelDecimalFormatter.format(v)))})(sp =>
            getDv[A, Option[String]](semantics, "tag", Some(s"Option($sp)"), Some(None))
        )
    }
}
