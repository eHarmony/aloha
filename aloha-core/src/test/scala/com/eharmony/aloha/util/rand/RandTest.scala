package com.eharmony.aloha.util.rand

import java.util.Random

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.annotation.tailrec

/**
  * Test the randomness of the random!
  *
  * @author deaktator
  * @since  11/3/2017
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class RandTest extends Rand {
  import RandTest._

  @Test def testSampleCombinationProbabilities(): Unit = {
    val trials = 20
    val maxN = 5
    val r = new Random(0x105923abdd8L)

    val results = Iterator.fill(trials){
      val initSeed = r.nextLong()
      val samples = 1000 + r.nextInt(9001)
      val n = r.nextInt(maxN + 1)
      val k = r.nextInt(n + 1)
      val sd = samplingDist(initSeed, samples, n, k)
      (samples, n, k, sd)
    }

    checkResult(results)
  }

  /**
    * For any `n` and `k`, `choose(n, k)` states should be sampled with uniform probability.
    * @param results different `numSamples`, `n`, `k`, and ''sample probabilities'' for
    *                each state sampled.
    * @tparam A type of random variable.
    */
  private def checkResult[A](results: Iterator[(NumSamples, N, K, Distribution[A])]): Unit = {
    results.foreach { case (samples, n, k, statesAndProbs) =>
      // This can be driven down by increasing `samples`.
      // This is proportional to 1 / sqrt(samples).
      // So, if samples is 10000, this is about 10%.
      val pctDiffFromExpPr = 100 * (10 / math.sqrt(samples))

      val expStates = choose(n, k)
      val expPr = 1d / expStates

      // Check that all states are sampled.
      assertEquals(expStates, statesAndProbs.size)

      statesAndProbs.foreach { case (state, pr) =>
        // Check probabilities are within reason.
        assertEquals(s"for key '$state'", expPr, pr, expPr * (pctDiffFromExpPr / 100))
      }
    }
  }

  private def samplingDist(initSeed: Long, samples: Int, n: Int, k: Int): Distribution[String] =
    drawSamples(initSeed, samples, n, k).mapValues { c => c / samples.toDouble }

  private def drawSamples(initSeed: Long, samples: Int, n: Int, k: Int): Map[String, NumSamples] = {
    val seed = initSeedScramble(initSeed)

    val samplesAndSeed =
      (1 to samples)
        .foldLeft((Map.empty[String, Int], seed)){ case ((m, s), _) =>
          val (ind, newSeed) = sampleCombination(n, k, s)
          val key = ind.sorted.mkString(",")
          val updatedMap = m + (key -> (m.getOrElse(key, 0) + 1))
          (updatedMap, newSeed)
        }

    // Throw away final seed.
    samplesAndSeed._1
  }

  private def choose(n: Int, k: Int): Long = {
    @tailrec def fact(n: Int, p: Long = 1): Long =
      if (n <= 1) p else fact(n - 1, n * p)

    fact(n) / (fact(k) * fact(n-k))
  }
}

object RandTest {
  private type NumSamples = Int
  private type N = Int
  private type K = Int
  private type Distribution[A] = Map[A, Double]
}
