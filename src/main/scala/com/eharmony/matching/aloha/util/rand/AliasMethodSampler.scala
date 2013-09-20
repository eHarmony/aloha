package com.eharmony.matching.aloha.util.rand

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


    /** Get the aliases and probabilities.  Current implementation based on
      *
      - https://hips.seas.harvard.edu/blog/2013/03/03/the-alias-method-efficient-sampling-with-many-discrete-outcomes/
      *
      * Previous implementation based on:
      *
      - http://www.keithschwarz.com/interesting/code/alias-method/AliasMethod.java.html
      - http://www.keithschwarz.com/darts-dice-coins/
      *
      * There was a bug that occurred infrequently which seems to have been a problem that arose while porting.  It
      * was easier just to reimplement than to track down the problem.
      *
      * @param probs probabilities with which the sampler will return the associated index.
      * @return
      */
    protected[this] final def structures(probs: Seq[Double]): (IndexedSeq[Int], IndexedSeq[Double]) = {
        val k = probs.size
        require(0 < k, "prob must have at least one element")

        val z = k / probs.sum // Ensure normalization.

        val q = new Array[Double](k)
        val j = new Array[Int](k)

        var smaller: List[Int] = Nil
        var larger: List[Int] = Nil

        var kk = 0
        probs.foreach(prob => {
            q(kk) = z * prob
            if (q(kk) < 1)
                smaller = kk :: smaller
            else larger = kk :: larger
            kk += 1
        })

        while(smaller.nonEmpty && larger.nonEmpty) {
            val small = smaller.head
            smaller = smaller.tail

            val large = larger.head
            larger = larger.tail

            j(small) = large
            q(large) -= 1 - q(small)

            if (q(large) < 1)
                smaller = large :: smaller
            else larger = large :: larger
        }

        // Fixes for numerical instability.  Wouldn't be necessary in a perfect world.
        smaller.foreach {q(_) = 1}
        larger.foreach {q(_) = 1}

        (j.toIndexedSeq, q.toIndexedSeq)
    }

    /** Induces a non-uniform distribution given a uniform random variates.
      *
      * This method has the following nice property for distributions with two possible outcomes:
      *
      * Given two distributions ''D'',,1,, and ''D'',,2,, with two classes, if
      *
      -  ''y'' = ''Sampler''(''D'',,1,,).''sample''(''j'', ''x'')  ''AND''
      -  &Sigma;,,i &le; j,,(''D'',,1,i,,) = &Sigma;,,i &le; j,,(''D'',,2,i,,)
      *
      * then
      *
      * ''y'' = ''Sampler''(''D'',,2,,).''sample''(''j'', ''x'')
      *
      * The same guarantee doesn't hold when the number of classes is greater than two.
      *
      * '''Note''' the implication of this is that if this method is to be used to assign a treatment to an certain
      * population, the treatment should be assigned in the first (zeroth) class and the lack of treatment should be
      * indicated by subsequent class indices.  For instance:
      *
      * {{{
      * val (x, r) = getHashFromUser(u) // Some hashing function based on user info.
      *
      * val samplerFor20130920 = new IntAliasMethodSampler(Seq(0.05, 0.95))
      * val samplerFor20130921 = new IntAliasMethodSampler(Seq(0.10, 0.90))
      *
      * val msg = """
      *             | If previously assigned class 0, and the probability of inclusion is increased,
      *             | then subsequent assignment to class 0 will occur
      *           """.stripMargin.trim
      * assert(!(0 == samplerFor20130920.sample(x, r)) || (0 == samplerFor20130921.sample(x, r)), msg)
      * }}}
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
