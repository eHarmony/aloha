package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.density.{Dense, Sparse}

import scala.collection.{immutable => sci, mutable => scm}
import scala.reflect.{ClassTag, classTag}

final case class SparseFeatureExtractorFunction[A](features: sci.IndexedSeq[(String, GenAggFunc[A, Iterable[(String, Double)]])])
extends FeatureExtractorFunction[A, Iterable[(String, Double)]] {
    protected[this] val postProcessingFunction = (name: String, b: Sparse) => b.map(p => (name + p._1, p._2))
    protected[this] implicit def ctB(): ClassTag[Sparse] = classTag[Sparse]
}

final case class DenseFeatureExtractorFunction[A](features: sci.IndexedSeq[(String, GenAggFunc[A, Double])])
    extends FeatureExtractorFunction[A, Double] {
    protected[this] val postProcessingFunction = (_: String, b: Dense) => b
    protected[this] implicit def ctB(): ClassTag[Dense] = classTag[Dense]
}

final case class StringFeatureExtractorFunction[A](features: sci.IndexedSeq[(String, GenAggFunc[A, String])])
    extends FeatureExtractorFunction[A, String] {
    protected[this] val postProcessingFunction = (_: String, b: String) => b
    protected[this] implicit def ctB(): ClassTag[String] = classTag[String]
}

/**
 * A function that takes a value and returns extracted features and information on missing and erring features.
 * @tparam A
 * @tparam Density
 */
trait FeatureExtractorFunction[A, @specialized(Double) Density] extends (A => (MissingAndErroneousFeatureInfo, IndexedSeq[Density])) {
    val features: sci.IndexedSeq[(String, GenAggFunc[A, Density])]
    protected[this] val postProcessingFunction: (String, Density) => Density
    protected[this] implicit def ctB(): ClassTag[Density]

    /**
     * @param a an input
     * @return information about which feature names are missing or in err in addition to the expanded features.
     */
    def apply(a: A): (MissingAndErroneousFeatureInfo, IndexedSeq[Density]) = {
        def h(l: Array[Density], i: Int, n: Int, missing: List[String], erring: List[String]): (MissingAndErroneousFeatureInfo, IndexedSeq[Density]) = {
            if (i >= n)
                (MissingAndErroneousFeatureInfo(missing.reverse, erring.reverse), l)
            else {
                val (name, feature) = features(i)
                // Prefix the feature value tuple key by the feature name.
                l(i) = postProcessingFunction(name, feature(a))
                val problems = feature.accessorOutputProblems(a)
                h(l, i + 1, n,
                    if (problems.missing.nonEmpty) name :: missing else missing,
                    if (problems.errors.nonEmpty) name :: erring else erring)
            }
        }

        val n = features.size
        implicit val b = ctB() // Need this so that the class tag can be found so that the array can be created.
        h(new Array[Density](n), 0, n, Nil, Nil)
    }
}
