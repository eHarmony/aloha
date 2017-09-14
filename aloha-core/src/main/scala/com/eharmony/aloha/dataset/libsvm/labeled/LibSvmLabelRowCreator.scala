package com.eharmony.aloha.dataset.libsvm.labeled

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.libsvm.labeled.json.LibSvmLabeledJson
import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmRowCreator
import com.eharmony.aloha.dataset._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.Logging
import com.eharmony.aloha.util.hashing.HashFunction
import spray.json.JsValue

import scala.util.Try

class LibSvmLabelRowCreator[-A](
        covariates: FeatureExtractorFunction[A, Sparse],
        label: GenAggFunc[A, String],
        hash: HashFunction,
        numBits: Int = LibSvmRowCreator.DefaultBits)
extends LibSvmRowCreator[A](covariates, hash, numBits)
   with LabelRowCreator[A, CharSequence] {

    override def apply(data: A): (MissingAndErroneousFeatureInfo, CharSequence) = {
        val (missing, iv) = super.apply(data)
        val lab = label(data)
        val sb = new StringBuilder().append(lab).append(" ").append(iv)
        (missing, sb)
    }

    override def stringLabel: GenAggFunc[A, Option[String]] = label.andThenGenAggFunc(Option.apply)
}

object LibSvmLabelRowCreator {
    final case class Producer[A]()
        extends RowCreatorProducer[A, CharSequence, LibSvmLabelRowCreator[A]]
        with RowCreatorProducerName
        with SparseCovariateProducer
        with DvProducer
        with CompilerFailureMessages
        with Logging {

        type JsonType = LibSvmLabeledJson
        def parse(json: JsValue): Try[LibSvmLabeledJson] = Try { json.convertTo[LibSvmLabeledJson] }
        def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: LibSvmLabeledJson): Try[LibSvmLabelRowCreator[A]] = {
            val hash: HashFunction = jsonSpec.salt match {
                case Some(s) => new com.eharmony.aloha.util.hashing.MurmurHash3(s)
                case None    => com.eharmony.aloha.util.hashing.MurmurHash3
            }

            val spec =
                for {
                    label <- getLabel(semantics, jsonSpec)
                    cov <- getCovariates(semantics, jsonSpec)
                    spec = jsonSpec.numBits match {
                        case Some(b) => new LibSvmLabelRowCreator(cov, label, hash, b)
                        case _       => new LibSvmLabelRowCreator(cov, label, hash)
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
}
