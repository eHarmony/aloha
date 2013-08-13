package com.eharmony.matching.aloha.util.rand

import scala.collection.mutable

trait AliasMethodSampler {

    /** The alias table used in the alias method.
      */
    protected[this] val alias: IndexedSeq[Int]

    /** The probability table used in the alias method.
      */
    protected[this] val probabilities: IndexedSeq[Double]

    /** The number of different classes possible.
      */
    val numClasses: Int

    /** Get the aliases and probabilities.
      * @param prob probabilities with which the sampler will return the associated index.
      * @return
      */
    protected[this] final def structures(prob: Seq[Double]): (IndexedSeq[Int], IndexedSeq[Double]) = {
        val n = prob.size
        require(0 < n, "prob must have at least one element")

        val a = new Array[Int](n)

        /* Compute the average probability and cache it for later use. */
        val average = 1.0 / n

        val z = prob.sum
        val pr = Array(prob.map(_ / z):_*)

        // Create two stacks to act as worklists as we populate the tables.
        val (large, small) = {
            val (l, s) = pr.zipWithIndex.partition(_._1 >= average)
            (mutable.Stack(l.map(_._2):_*), mutable.Stack(s.map(_._2):_*))
        }

        while (small.nonEmpty && large.nonEmpty) {
            val less = small.pop()          // Get the index of the small and the large probabilities.
            val more = large.pop()

            pr(less) *= n                   // These probabilities have not yet been scaled up to be such that
            a(less) = more                  // 1/n is given weight 1.0.  We do this here instead.

            pr(more) += pr(less) - average  // Decrease the probability of the larger one by the appropriate amount.

            // If the new pr is less than the average, add it into the small list; otherwise, add it to the large list.
            if (pr(more) < average) small.push(more) else large.push(more)
        }

        // At this point, everything is in one list, which means that the remaining probabilities should all be 1/n.
        // Based on this, set them appropriately.  Due to numerical issues, we can't be sure which stack will hold
        // the entries, so we empty both.
        while (small.nonEmpty) pr(small.pop()) = 1.0
        while (large.nonEmpty) pr(large.pop()) = 1.0

        (a.toIndexedSeq, pr.toIndexedSeq)
    }

    /** Induces a non-uniform distribution given a uniform random variate.
      *
      * @param uniformlyDistributedVariate integer value in {0, 1, ..., numClasses - 1}.
      *
      *                                    '''NOTE''': the distribution of values passed to this function ''MUST BE''
      *                                    uniformly distributed.  Otherwise, the sampling will be biased.
      * @param uniform01 a real-valued uniform variate in the [0, 1] interval.
      * @return
      */
    def sample(uniformlyDistributedVariate: Int, uniform01: Float) = {

        // Generate a biased coin toss to determine which option to pick.
        val coinToss = uniform01 < probabilities(uniformlyDistributedVariate)

        // Based on the outcome, return either the column or its alias.
        val k = if (coinToss) uniformlyDistributedVariate else alias(uniformlyDistributedVariate)

        k
    }
}
