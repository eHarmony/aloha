package com.eharmony.aloha.util.rand

/**
  * Some stateless random sampling utilities.
  *
  * @author deaktator
  * @since 11/3/2017
  */
private[aloha] trait Rand {

  type Seed = Long
  type Index = Short

  /**
    * Perform the initial scramble.  This should be called '''''once''''' on the initial
    * seed prior to the first call to `sampleCombination`.
    * @param seed an initial seed
    * @return a more scrambled seed.
    */
  protected def initSeedScramble(seed: Seed): Seed =
    (seed ^ 0x5DEECE66DL) & 0xFFFFFFFFFFFFL


  /**
    * Sample a ''k''-combination from a population of ''n''.
    *
    * This algorithm uses a linear congruential pseudorandom number generator (see Knuth)
    * to perform reservoir sampling via "''Algorithm R''".
    *
    * It is ~ '''''O'''''('''''n''''').
    *
    * If `n` ≤ `k`, then return 0, ..., `n` - 1; otherwise, if `k` < `n`, the
    * returned array have length `k` with values between 0 and `n - 1` (inclusive)
    * but it is '''NOT''' guaranteed to be sorted.
    *
    * '''NOTE''': This is a pure function.  It produces the same results as if
    * `java.util.Random` was used to perform reservoir sampling but since it doesn't
    * carry state, this can be trivially operated in parallel with no locking or CAS
    * loop overhead.  The consequence is that the `seed` must be provided on every call
    * and a new seed will be returned as part of the output.
    *
    * To get this function to act like `java.util.Random`, the first time it is called, the
    * seed should be produce by running the desired seed through `initSeedScramble`.  For
    * instance:
    *
    * {{{
    * val (kComb1, newSeed1) = sampleCombination(4, 2, initSeedScramble(0))
    * val (kComb2, newSeed2) = sampleCombination(4, 2, newSeed1)
    * }}}
    *
    * For more information, see:
    *
    - [[http://luc.devroye.org/chapter_twelve.pdf Non-Uniform Random Variate Generation, Luc Devroye, Ch. 12]]
    - [[https://en.wikipedia.org/wiki/Reservoir_sampling#Algorithm_R Reservoir sampling (Wikipedia)]]
    - [[http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/java/util/Random.java grepcode for Random]]
    - [[https://en.wikipedia.org/wiki/Linear_congruential_generator Linear congruential generator (Wikipedia)]]
    *
    * @param n population size (< 2^15^).
    * @param k combination size (< 2^15^).
    * @param seed the seed to use for random selection
    * @return a tuple 2 containing the array of 0-based indices representing
    *         the ''k''-combination and a new random seed.
    */
  protected def sampleCombination(n: Int, k: Int, seed: Seed): (Array[Index], Seed) = {

    // NOTE: This isn't idiomatic Scala code but it will operate in hot loops in minimizing
    //       object creation is important.

    if (n <= k) {
      ((0 until n).map(i => i.toShort).toArray, seed)
    }
    else {
      var i = k + 1
      var nextSeed = seed
      var reservoirSwapInd = 0
      var bits = 0
      var value = 0

      // Fill reservoir with the first k indices.
      val reservoir = (0 until k).map(i => i.toShort).toArray

      // Loop over the rest of the indices outside the reservoir and determine if
      // swapping should occur.  If so, swap the index in the reservoir with the
      // current element, i - 1.
      while (i <= n) {
        reservoirSwapInd =
          if ((i & -i) == i) {
            // i = 2^j, for some j.

            // To understand these constants, see
            // https://en.wikipedia.org/wiki/Linear_congruential_generator
            nextSeed = (nextSeed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL

            // 17 = log2(modulus) - shift = 48 - 31
            ((i * (nextSeed >>> 17)) >> 31).toInt
          }
          else {
            // Loop at least once per swap index.
            do {
              nextSeed = (nextSeed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
              bits = (nextSeed >>> 17).toInt
              value = bits % i
            } while (bits - value + (i - 1) < 0)

            value
          }

        // This is the key to maintaining the proper probabilities in the reservoir sampling.
        // Wikipedia does a good job describing this in the proof by induction in the
        // explanation for Algorithm R.
        if (reservoirSwapInd < k)
          reservoir(reservoirSwapInd) = (i - 1).toShort

        i += 1
      }

      (reservoir, nextSeed)
    }
  }
}
