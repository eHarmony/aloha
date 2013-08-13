package com.eharmony.matching.aloha.util.rand

import scala.util.hashing.MurmurHash3

object HashedCategoricalDistribution {
    private val MaxVal = Int.MaxValue.toFloat

    def apply(probabilities: Double*): HashedCategoricalDistribution =
        new HashedCategoricalDistribution(new IntAliasMethodSampler(probabilities))
}

class HashedCategoricalDistribution(sampler: IntAliasMethodSampler) extends (TraversableOnce[Any] => Int) {

    var ps = List.empty[Float]
    var ks = List.empty[Int]

    import HashedCategoricalDistribution.MaxVal

    /** The number of different classes in the distribution.
      */
    val numClasses: Int = sampler.numClasses

    /** Pick a number in {0, 1, ..., numClasses - 1} with the probabilities passed to the sampler given as a
      * constructor argument.  This works by passing ''k'' and ''f'' to the sampler instance, where ''k'' and ''f''
      * are defined as:
      *
      * {{{
      * val MaxVal = Int.MaxValue.toFloat
      * val h = MurmurHash3 orderedHash data
      * val hAbs = math.abs(h)
      * val k = hAbs % numClasses
      * val f = hAbs / MaxVal
      * }}}
      *
      * It is assumed that MurmurHash3.orderedHash produces hashes whose bits are uniformly distributed.
      * @param data
      * @return
      */
    def apply(data: TraversableOnce[Any]): Int = {
        val h = MurmurHash3 orderedHash data

        // Take the absolute value because we want (hAbs % sampler.getNumClasses) to give a non-negative index.  This
        // also makes the computation of f easier.  It will lower the entropy of the output of the hash function but
        // this is acceptable.
        val hAbs = math.abs(h)
        val p = hAbs / MaxVal
        val k = hAbs % numClasses
        ps = p :: ps
        ks = k :: ks
        sampler.sample(k, p)
    }
}
