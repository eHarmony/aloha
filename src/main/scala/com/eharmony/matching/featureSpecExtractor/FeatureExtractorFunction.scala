package com.eharmony.matching.featureSpecExtractor

import scala.collection.{immutable => sci, mutable => scm}
import com.eharmony.matching.aloha.semantics.func.GenAggFunc

/**
 * A function that takes a value and returns extracted features and information on missing and erring features.
 * @param features (name, function) key-value pairs.
 * @tparam A
 */
case class FeatureExtractorFunction[-A](features: sci.IndexedSeq[(String, GenAggFunc[A, Iterable[(String, Double)]])])
    extends (A => (MissingAndErroneousFeatureInfo, sci.IndexedSeq[Iterable[(String, Double)]])) {

    /**
     * @param a an input
     * @return information about which feature names are missing or in err in addition to the expanded features.
     */
    def apply(a: A): (MissingAndErroneousFeatureInfo, IndexedSeq[Iterable[(String, Double)]]) = {
        def h(l: Array[Iterable[(String, Double)]], i: Int, n: Int, missing: List[String], erring: List[String]): (MissingAndErroneousFeatureInfo, IndexedSeq[Iterable[(String, Double)]]) = {
            if (i >= n)
                (MissingAndErroneousFeatureInfo(missing, erring),
                 new scm.WrappedArray.ofRef[Iterable[(String, Double)]](l))
            else {
                val (name, feature) = features(i)
                // Prefix the feature value tuple key by the feature name.
                l(i) = feature(a).map(p => (name + p._1, p._2))
                val problems = feature.accessorOutputProblems(a)
                h(l, i + 1, n,
                    if (problems.missing.nonEmpty) name :: missing else missing,
                    if (problems.errors.nonEmpty) name :: erring else erring)
            }
        }

        val n = features.size
        h(new Array[Iterable[(String, Double)]](n), 0, n, Nil, Nil)
    }
}
