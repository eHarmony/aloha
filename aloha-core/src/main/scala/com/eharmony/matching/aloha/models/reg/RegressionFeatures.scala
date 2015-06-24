package com.eharmony.matching.aloha.models.reg

import com.eharmony.matching.aloha.semantics.func.GenAggFunc

import scala.collection.{immutable => sci, mutable => scm}

/**
 * A helper trait for sparse regression models with String keys.  This trait exposes the ''constructFeatures''
 * method which applies the ''featureFunctions'' to the input data and keeps track of missing features.
 * @author R M Deak
 */
// TODO: merge this with com.eharmony.matching.featureSpecExtractor.FeatureExtractorFunction
trait RegressionFeatures[-A] {

    /**
     * Parallel to featureFunctions.
     */
    protected[this] val featureNames: sci.IndexedSeq[String]

    /**
     * Parallel to featureNames.  This is the sequence of functions that extract data from the input value.
     */
    protected[this] val featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]]

    /**
     * A threshold dictating how many missing features to allow before making the prediction fail.  None means
     * the threshold is &infin;.  If, when mapping featureFunctions over the input, the resulting sequence
     * contains more than ''numMissingThreshold'' values that are empty Iterable values, then the
     * ''Features.missingOk'' value returned by ''constructFeatures'' will be '''false'''; otherwise, it will
     * be '''true'''.
     */
    protected[this] val numMissingThreshold: Option[Int]

    /**
     * Container for information returned by [[RegressionFeatures.constructFeatures]].  Note that as is,
     * this declaration will cause a compiler warning:
     *
     *     "The outer reference in this type test cannot be checked at run time."
     *
     * This is a known issue and is a scala bug.  See:
     * - https://issues.scala-lang.org/browse/SI-4440
     * - http://stackoverflow.com/questions/16450008/typesafe-swing-events-the-outer-reference-in-this-type-test-cannot-be-checked-a
     *
     * A solution that would remove the warning is to make the class not ''final''.  Not doing this just to remove a
     * warning.
     * @param features features that were extracted from an input value.
     * @param missing map from feature name to variables in the feature function that were missing.
     * @param missingOk whether the number of
     */
    protected[this] case class Features[F](features: F,
                                           missing: scm.Map[String, Seq[String]] = scm.Map.empty,
                                           missingOk: Boolean = true)

    /**
     * Extract the features from the raw data by mapping ''featureFunctions'' over the input.  If
     * ''numMissingThreshold'' is not None and the number of resulting empty Iterables exceeds the
     * ''numMissingThreshold'' value, then the resulting ''Features.missingOk'' value is '''false''';
     * otherwise, it will be '''true'''.  If ''Features.missingOk'' is '''false''', then go back and
     * check all feature functions for missing values and add findings to the ''Features.missing''
     * map.  This ''Features.missing'' is a mapping from the feature specification to the list of
     * variable names whose associated values are missing from the input.
     *
     * @param a raw input data of the model input type.
     * @return a Features instance with the following:
     *           1 the transformed input vector
     *           1 the map of bad features to the missing values in the raw data that were needed to compute the feature
     *           1 whether the amount of missing data is acceptable to still continue
     */
    protected[this] final def constructFeatures(a: A): Features[IndexedSeq[Iterable[(String, Double)]]] = {
        // NOTE: Since this function is at the center of the regression process and will be called many times, it
        //       needs to be efficient.  Therefore, it uses some things that are not idiomatic scala.  For instance,
        //       there are mutable variables, while loops instead of for comprehensions or Range.foreach, etc.

        val missing = scm.Map.empty[String, Seq[String]]
        val n = featureNames.size
        val f = new Array[Iterable[(String, Double)]](n)
        var i = 0
        while (i < n) {
            // Use concat based on
            //   http://stackoverflow.com/questions/5076740/whats-the-fastest-way-to-concatenate-two-strings-in-java
            //   http://stackoverflow.com/questions/47605/string-concatenation-concat-vs-operator
            f(i) = featureFunctions(i)(a).map(p => (featureNames(i).concat(p._1), p._2))

            // If the feature is empty, it can't contribute to the inner product.  If it can't contribute to the
            // inner product but appears in the inner product specification, there are two possibilities:
            //
            //   1) The specifier doesn't care about performance and needlessly added a NoOp.
            //   2) The feature could emit a value because data necessary to do so is missing.
            //
            // In either case, we take those opportunities to check for missing data and assume the performance
            // hit is acceptable.
            if (f(i).isEmpty)
                missing += (featureFunctions(i).specification -> featureFunctions(i).accessorOutputMissing(a))

            i += 1
        }

        val numMissingOk = numMissingThreshold map { missing.size <= _ } getOrElse true

        // If we are going to err out, allow a linear scan (with repeated work so that we can get richer error
        // diagnostics.  Only include the values where the list of missing accessors variables is not empty.
        // This could have been done with a for comprehension but this is a little faster.
        if (!numMissingOk) {
            0 until n foreach { i =>
                val miss = featureFunctions(i).accessorOutputMissing(a)
                if (miss.nonEmpty)
                    missing += featureFunctions(i).specification -> miss
            }
        }

        Features(new collection.mutable.WrappedArray.ofRef(f), missing, numMissingOk)
    }
}
