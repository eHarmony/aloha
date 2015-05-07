package com.eharmony.matching.featureSpecExtractor.csv

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.csv.json.CsvJson
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, DenseCovariateProducer, SpecProducer}
import spray.json.JsValue

import scala.util.Try

case class CsvSpecProducer[A](separator: String)
extends SpecProducer[A, CsvSpec[A]]
   with DenseCovariateProducer
   with CompilerFailureMessages {

    def this() = this(CsvSpecProducer.Separator)

    type JsonType = CsvJson
    def name = getClass.getSimpleName
    def parse(json: JsValue): Try[CsvJson] = Try { json.convertTo[CsvJson] }
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: CsvJson): Try[CsvSpec[A]] = {
        val spec = getCovariates(semantics, jsonSpec) map { cov => CsvSpec(cov, separator) }
        spec
    }
}

object CsvSpecProducer {
    private val Separator = ","
}
