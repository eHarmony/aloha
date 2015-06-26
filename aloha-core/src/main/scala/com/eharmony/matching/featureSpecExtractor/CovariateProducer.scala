package com.eharmony.matching.featureSpecExtractor

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.density.{Dense, Sparse}
import com.eharmony.matching.featureSpecExtractor.json.CovariateJson

import scala.collection.immutable.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}


/**
 * Helper to produce functions used to the create the covariate data passed to the dataset generators.
 */
sealed trait CovariateProducer[@specialized(Double) Density] { self: CompilerFailureMessages =>
    protected[this] def featExtFuncProd[A](successes: IndexedSeq[(String, GenAggFunc[A, Density])]): FeatureExtractorFunction[A, Density]
    protected[this] def refInfoB(): RefInfo[Density]

    protected[this] def getCovariates[A](semantics: CompiledSemantics[A], cj: CovariateJson[Density], defDefault: Option[Density] = None): Try[FeatureExtractorFunction[A, Density]] = {
        // Get a new semantics with the imports changed to reflect the imports from the Json Spec
        // Import of ExecutionContext.Implicits.global is necessary.
        val semanticsWithImports = semantics.copy[A](imports = cj.imports)

        def compile(it: Iterator[json.Spec[Density]], successes: List[(String, GenAggFunc[A, Density])]): Try[FeatureExtractorFunction[A, Density]] = {
            if (!it.hasNext)
                Success{ featExtFuncProd(successes.reverse.toIndexedSeq) }
            else {
                val spec = it.next()
                val f = semanticsWithImports.createFunction[Density](spec.spec, spec.defVal orElse defDefault)(refInfoB())
                f match {
                    case Left(msgs) => Failure { failure(spec.name, msgs) }
                    case Right(success) => compile(it, (spec.name, success) :: successes)
                }
            }
        }

        compile(cj.features.iterator, Nil)
    }
}

trait SparseCovariateProducer extends CovariateProducer[Iterable[(String, Double)]] { self: CompilerFailureMessages =>
    protected[this] final def featExtFuncProd[A](successes: IndexedSeq[(String, GenAggFunc[A, Sparse])]) = SparseFeatureExtractorFunction(successes)
    protected[this] final def refInfoB() = RefInfo[Sparse]
}

trait DenseCovariateProducer extends CovariateProducer[Double] { self: CompilerFailureMessages =>
    protected[this] final def featExtFuncProd[A](successes: IndexedSeq[(String, GenAggFunc[A, Dense])]) = DenseFeatureExtractorFunction(successes)
    protected[this] final def refInfoB() = RefInfo[Dense]
}

