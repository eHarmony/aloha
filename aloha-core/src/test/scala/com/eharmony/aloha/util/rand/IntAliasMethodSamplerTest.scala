package com.eharmony.aloha.util.rand

import scala.language.postfixOps
import scala.util.Random

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._
import scala.util.hashing.MurmurHash3
import com.eharmony.aloha.JensenShannonDivergence

@RunWith(classOf[BlockJUnit4ClassRunner])
class IntAliasMethodSamplerTest {
    private[this] val normalizer = (Int.MaxValue.toLong - Int.MinValue).toFloat


    /** Distributions that gave previous alias method sampling implementation problems.
      */
    @Test def testAccuracyHardDistributions() {
        val numSamples = 1000000

        val distributions = Seq(
            normalize(List.fill(1)(1)),
            normalize(List.fill(5)(1)),
            List(0.05309346425588812, 0.5100428950619801, 0.4368636406821319),
            List(0.40141036292241195, 0.051249057422684485, 0.5473405796549036),
            List(7.195E-4, 0.0017872, 0.0058057, 0.4968644, 0.3965917, 0.0982315),
            List(0.0051010662328547654, 0.003196898773763772, 0.010903135623389536, 0.008529504496852573, 0.34078314654664515, 0.5926351394406623, 0.03885110888583177),
            List(0.011037747191864233, 0.1179023384424632, 0.20268990032590745, 0.09693384548043035, 0.2466383857966139, 0.2575669724211299, 0.06723081034159081),
            List(0.04333814305177907, 0.036178725801217206, 0.05009907306158789, 0.042179672992987256, 0.049430280979568426, 0.007434183991040165, 0.01686497394972107, 0.012620597807338135, 0.46915035629216656, 0.2727039920725942)
        )

        distributions.foreach(d => {
            val s = new IntAliasMethodSampler(d)
            val pr = new Array[Double](s.numClasses)
            var i = 0
            while (i < numSamples) {
                pr(classify(s, i)) += 1
                i += 1
            }
            pr.indices.foreach(i => pr(i) /= numSamples)
            JensenShannonDivergence.assertWithinTolerance(d.toArray, pr, numSamples)
        })
    }

    /** Ensure that 2-class distributions can be expanded and if the user is in the zeroth class, he still will be
      * after it's probability is increased.
      */
    @Test def test2ClassDistribution() {
        val numRandomSamples = 1000
        val prGranularity: Int = 97 // Sum prime
        val rand = new scala.util.Random(1)

        val distributions = List.range(1, prGranularity + 1).map(i => dist2(i / prGranularity.toDouble))

        distributions.tails.foreach {
            case testDist :: subsequentDists =>
                // Get a bunch of samples where the classes returned is class 0 (ala the 3rd argument)
                val samples = getSamples(rand, numRandomSamples, _ == 0, testDist)

                // Given that the probability distribution testDist is (p = i / prGranularity, 1 - p) for classes 0 and
                // 1, respectively, iterate over all such distributions (p = i1 / prGranularity, 1 - p) such that
                // i1 > i and ensure that given the same random variates, that if the 0 class was returned testDist
                // then the 0 class will be returned when the probability of the zero class is increased.
                for {
                    dist <- subsequentDists
                    iams = new IntAliasMethodSampler(dist)
                    sample <- samples
                } {
                    val Sample(d, r, result) = sample
                    assertEquals(result, iams.sample(d, r))
                }
            case _ => // Should happen one time because tails includes the empty list at the end.
        }
    }

    @Test def testRandom2ClassDistribution() {
        val rand = new scala.util.Random(2)
        (1 to 1000000).foreach(_ => test(2, 2, rand))
    }

    /** Sadly, this doesn't work.  We'd like to be able to guarantee this but it'll require an algorithm change that
      * would be cool enough to be paper-worthy.
      */
    @Test(expected = classOf[AssertionError])
    def randomClassDistributionSamePrefixAndRandomSuffix() {
        val rand = new scala.util.Random(3)
        (1 to 38).foreach(i => test(2, 3, rand))
    }

    /** Sadly, this doesn't work either.
      */
    @Test(expected = classOf[AssertionError])
    def randomClassDistributionWithSamePrefixAndBoostedProbAndScaledDownSubsequentProbs() {
        val rand = new scala.util.Random(4)
        (1 to 3).foreach(i => test1(2, 3, rand, Some(i)))
    }

    private[this] def test1(minSize: Int, maxSize: Int, rand: Random, testNumber: Option[Int] = None) {
        //   _ _ _ _ _ | _ | _ _ _ _ _
        //   _________   _   _________
        //   size = j    j
        //
        //   pr(0) ... pr(j-1) unchanged
        //   pr(j) increased
        //   pr(j+1) ... pr(n-1) randomly filled in
        //
        //   j >= 0
        //   j < n - 1
        val n = rand.nextInt(maxSize - minSize + 1) + minSize
        val j = rand.nextInt(n - 1)
        val x = rand.nextFloat()
        val sumPrefix = if (0 == j) 0.0 else rand.nextDouble()
        val complSumPrefix = 1 - sumPrefix
        val prefix = randomSeq(j, sumPrefix)(rand)

        val suff1 = randomSeq(n - j, complSumPrefix)(rand)
        val dist1 = prefix ++ suff1
        val jprOld = suff1.head
        val jpr = jprOld + rand.nextDouble() * (complSumPrefix - jprOld)

        val suff2 = normalize(suff1.tail, complSumPrefix - jpr)        // Scaled tail
//      val suff2 = randomSeq(n - j - 1, complSumPrefix - jpr)(rand)   // Random
        val dist2 = prefix ++ Seq(jpr) ++ suff2
        val s1 = new IntAliasMethodSampler(dist1)
        val s2 = new IntAliasMethodSampler(dist2)
        val v1 = s1.sample(j, x)
        val v2 = s2.sample(j, x)

        testNumber.fold(assertEquals(v1, v2))(i => assertEquals(s"on test $i", v1, v2))
    }

    private[this] def test(minSize: Int, maxSize: Int, rand: Random, testNumber: Option[Int] = None) {
        val n = rand.nextInt(maxSize - minSize + 1) + minSize
        val j = rand.nextInt(n)
        val jSize = j + 1
        val x = rand.nextFloat()
        val sumPrefix = rand.nextDouble()
        val prefix = randomSeq(jSize, sumPrefix)(rand)
        val dist1 = prefix ++ randomSeq(n - jSize, 1 - sumPrefix)(rand)
        val dist2 = prefix ++ randomSeq(n - jSize, 1 - sumPrefix)(rand)
        val s1 = new IntAliasMethodSampler(dist1)
        val s2 = new IntAliasMethodSampler(dist2)
        val v1 = s1.sample(j, x)
        val v2 = s2.sample(j, x)

        testNumber.fold(assertEquals(v1, v2))(i => assertEquals(s"on test $i", v1, v2))
    }

    @inline private[this] def classify(s: IntAliasMethodSampler, value: Int) = {
        val v = MurmurHash3.orderedHash(Seq(value, 1))
        val x = math.abs(v % s.numClasses)
        val pp = (v.toLong - Int.MinValue) / normalizer
        s.sample(x, pp)
    }

    private[this] def randomSeq(n: Int, sum: Double = 1)(implicit rand: Random) = {
        val xs = Seq.fill(n)(rand.nextDouble())
        val res = normalize(xs, sum)
        res
    }

    private[this] def normalize(xs: Seq[Double], targetSum: Double = 1) = {
        val z = targetSum / xs.sum
        xs.map(z *)
    }

    private[this] def dist2(pr: Double) = Seq(pr, 1 - pr)

    private[this] def getSamples(rand: Random, n: Int, filter: Int => Boolean, pr: Seq[Double]): Seq[Sample] = {
        val sampler = new IntAliasMethodSampler(pr)
        val d = pr.size
        var samples = List.empty[Sample]
        while(samples.size < n) {
            val ri = rand.nextInt(d)
            val rf = rand.nextFloat()
            val y = sampler.sample(ri, rf)
            if (filter(y)) {
                samples = Sample(ri, rf, y) :: samples
            }
        }
        samples.reverse // doesn't matter, but reverse since samples in reverse order of discovery
    }

    private[this] case class Sample(zeroToD: Int, zeroOne: Float, result: Int)
}
