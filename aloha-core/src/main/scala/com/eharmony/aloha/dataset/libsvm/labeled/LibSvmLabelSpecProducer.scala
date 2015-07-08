package com.eharmony.aloha.dataset.libsvm.labeled

import com.eharmony.aloha.dataset.libsvm.labeled.json.LibSvmLabeledJson
import com.eharmony.aloha.dataset.{CompilerFailureMessages, DvProducer, SparseCovariateProducer, SpecProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.Logging
import com.eharmony.aloha.util.hashing.HashFunction
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
        val hash: HashFunction = jsonSpec.salt match {
            case Some(s) => new com.eharmony.aloha.util.hashing.MurmurHash3(s)
            case None    => com.eharmony.aloha.util.hashing.MurmurHash3
        }

        val spec =
            for {
                label <- getLabel(semantics, jsonSpec)
                cov <- getCovariates(semantics, jsonSpec)
                spec = jsonSpec.numBits match {
                    case Some(b) => new LibSvmLabelSpec(cov, label, hash, b)
                    case _       => new LibSvmLabelSpec(cov, label, hash)
                }
            } yield {
                warn(hash.salts)
                spec
            }

        spec
    }

    private[this] def getLabel(semantics: CompiledSemantics[A], jsonSpec: LibSvmLabeledJson): Try[GenAggFunc[A, String]] =
        getDv(semantics, "label", Option(jsonSpec.label), Option(""))
}
