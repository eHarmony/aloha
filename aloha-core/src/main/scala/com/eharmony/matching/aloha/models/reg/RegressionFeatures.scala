package com.eharmony.matching.aloha.models.reg

import com.eharmony.matching.aloha.semantics.func.GenAggFunc

import scala.collection.{immutable => sci, mutable => scm}

/**
 * Created by rdeak on 6/1/15.
 */
trait RegressionFeatures[-A] {

    protected[this] val featureNames: sci.IndexedSeq[String]

    protected[this] val featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]]

    protected[this] val numMissingThreshold: Option[Int]

    /**
     * Container for information returned by [[RegressionFeatures.constructFeatures]]
     * @param features
     * @param missing
     * @param missingOk
     */
    final protected[this] case class Features[F](features: F,
                                              missing: scm.Map[String, Seq[String]],
                                              missingOk: Boolean)

    /**
     * Extract the features from the raw data.  Intentionally, ''protected[this] final'' so that we can extend this
     * class
     * @param a raw input data of the model input type.
     * @return a Tuple3 with the following:
     *           1 the transformed input vector
     *           1 the map of bad features to the missing values in the raw data that were needed to compute the feature
     *           1 whether the amount of missing data is acceptable to still continue
     */
    protected[this] final def constructFeatures(a: A): Features[IndexedSeq[Iterable[(String, Double)]]] = {
        val missing = scm.Map.empty[String, Seq[String]]
        val n = featureNames.size
        val f = new Array[Iterable[(String, Double)]](n)
        for (i <- 0 until n) {
            val name = featureNames(i)

            // We use concat based on http://stackoverflow.com/questions/5076740/whats-the-fastest-way-to-concatenate-two-strings-in-java
            f(i) = featureFunctions(i)(a).map(p => (name.concat(p._1), p._2))

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
        }

        val numMissingOk = numMissingThreshold map { missing.size < _ } getOrElse true

        // If we are going to err out, allow a linear scan (with repeated work so that we can get richer error
        // diagnostics.
        if (!numMissingOk) {
            for (i <- 0 until n) {
                missing += (featureFunctions(i).specification -> featureFunctions(i).accessorOutputMissing(a))
            }
        }

        Features(new collection.mutable.WrappedArray.ofRef(f), missing, numMissingOk)
    }
}
