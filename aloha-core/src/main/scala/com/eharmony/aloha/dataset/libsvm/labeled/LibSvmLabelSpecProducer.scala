package com.eharmony.aloha.dataset.libsvm.labeled

import com.eharmony.aloha.dataset.libsvm.labeled.json.LibSvmLabeledJson
import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmSpecProducer
import com.eharmony.aloha.dataset.{CompilerFailureMessages, DvProducer, SparseCovariateProducer, SpecProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.Logging
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
//        val sem = this.addStringImplicitsToSemantics(semantics, jsonSpec.imports)
//        getDv(sem, "label", Option(jsonSpec.label), Option(""))
        getDv(semantics, "label", Option(jsonSpec.label), Option(""))
    }
}
