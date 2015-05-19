package com.eharmony.matching.featureSpecExtractor.libsvm.labeled

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.Logging
import com.eharmony.matching.featureSpecExtractor._
import com.eharmony.matching.featureSpecExtractor.libsvm.labeled.json.LibSvmLabeledJson
import com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.LibSvmSpecProducer
import com.google.common.hash.Hashing.murmur3_32
import spray.json.JsValue

import scala.util.Try


final case class LibSvmLabelSpecProducer[A]()
    extends SpecProducer[A, LibSvmLabelSpec[A]]
    with SparseCovariateProducer
    with DvProducer
    with CompilerFailureMessages
    with Logging {

    type JsonType = LibSvmLabeledJson
    def name = getClass.getSimpleName
    def parse(json: JsValue): Try[LibSvmLabeledJson] = Try { json.convertTo[LibSvmLabeledJson] }
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: LibSvmLabeledJson): Try[LibSvmLabelSpec[A]] = {
        val salt = jsonSpec.salt.getOrElse(LibSvmSpecProducer.Salt)
        val spec =
            for {
                label <- getLabel(semantics, jsonSpec)
                cov <- getCovariates(semantics, jsonSpec)
                spec = jsonSpec.numBits match {
                    case Some(b) => new LibSvmLabelSpec(cov, label, murmur3_32(salt), b)
                    case _       => new LibSvmLabelSpec(cov, label, murmur3_32(salt))
                }
            } yield {
                warn(s"Created hashing function using: murmur3_32(salt = $salt)")
                spec
            }

        spec
    }

    private[this] def getLabel(semantics: CompiledSemantics[A], jsonSpec: LibSvmLabeledJson): Try[GenAggFunc[A, String]] = {
        val sem = this.addStringImplicitsToSemantics(semantics, jsonSpec.imports)
        getDv(sem, "label", Option(jsonSpec.label), Option(""))
    }
}
