package com.eharmony.matching.featureSpecExtractor

import scala.concurrent.ExecutionContext.Implicits.global

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.json.JsonSpec
import scala.util.{Failure, Success, Try}

/**
 * Helper to produce functions used to the create the covariate data passed to the dataset generators.
 */
trait CovariateProducer { self: CompilerFailureMessages =>
    protected[this] def getCovariates[A](semantics: CompiledSemantics[A], jsonSpec: JsonSpec): Try[FeatureExtractorFunction[A]] = {
        // Get a new semantics with the imports changed to reflect the imports from the Json Spec
        // Import of ExecutionContext.Implicits.global is necessary.
        val semanticsWithImports = semantics.copy[A](imports = jsonSpec.imports)

        def compile(it: Iterator[json.Spec], successes: List[(String, GenAggFunc[A, Iterable[(String, Double)]])]): Try[FeatureExtractorFunction[A]] = {
            if (!it.hasNext) Success{ FeatureExtractorFunction(successes.reverse.toIndexedSeq) }
            else {
                val spec = it.next()
                val f = semanticsWithImports.createFunction[Iterable[(String, Double)]](spec.spec, spec.defVal)
                f match {
                    case Left(msgs) => Failure { failure(spec.name, msgs) }
                    case Right(success) => compile(it, (spec.name, success) :: successes)
                }
            }
        }

        compile(jsonSpec.features.iterator, Nil)
    }
}
