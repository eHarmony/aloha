package com.eharmony.aloha.dataset.libsvm.unlabeled

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.{FeatureExtractorFunction, MissingAndErroneousFeatureInfo, Spec}
import com.google.common.hash.HashFunction

import scala.collection.{immutable => sci}

class LibSvmSpec[-A](covariates: FeatureExtractorFunction[A, Sparse], hash: HashFunction, numBits: Int = LibSvmSpec.DefaultBits) extends Spec[A] {
    require(1 <= numBits && numBits <= 31, s"numBits must be in {1, 2, ..., 31}.  Found $numBits")

    private[this] val mask = (1 << numBits) - 1

    def apply(data: A): (MissingAndErroneousFeatureInfo, CharSequence) = {
        val (missingInfo, cov) = covariates(data)

        // Get the features into key-value pairs where the keys are the hashed strings and the values
        // are the original values.  Make sure to sort.
        //
        // Use aggregate for efficiency.  For intuition, see http://stackoverflow.com/a/6934302/4484580
        //
        // TODO: Figure out parallel strategy.
        // For cov with large outer sequence size and small inner sequence size, non parallel is faster.
        // For cov with small outer sequence size and large inner sequence size, parallel (cov.par) is faster.
        // Both of these seem to be faster than flat mapping over cov like:
        //
        //   val f = for ( xs <- cov; (k, v) <- xs ) yield (hash.newHasher.putString(k).hash.asInt & mask, v)
        //   val sortedDeduped = sci.OrderedMap(f:_*)
        //
        val sortedDeduped = cov.aggregate(sci.SortedMap.empty[Int, Double])(
            (z, kvs) => kvs.foldLeft(z){ case (m, (k, v)) => m + ((hash.newHasher.putString(k).hash.asInt & mask, v)) },
            (m1, m2) => m1 ++ m2
        )

        val out = sortedDeduped.view.map { case(k, v) => s"$k:$v" }.mkString(" ")
        (missingInfo, out)
    }
}

object LibSvmSpec {
    val DefaultBits = 18
}
