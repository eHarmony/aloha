package com.eharmony.aloha.util.rand

import java.util.Random

import org.junit.Test
import org.junit.Assert.fail
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
    val failures = findFailures(
      trials     = 25,
      maxN       = 6,
      minSamples = 1000,
      maxSamples = 10000,
      seed       = 0
    )

    val failureMsg = allFailuresMsg(failures)
    failureMsg foreach fail
  }

  /**
    * Sets up the sampling scenarios.
    * @param trials number of trials to run.
    * @param maxN maximum N value
    * @param minSamples minimum number of samples to draw per trial.
    * @param maxSamples maximum number of samples to draw per trial.
    * @param seed a random seed to use for generating the parameters in the scenarios.
    * @return sampling scenarios
    */
  private def samplingScenarios(
      trials: Int,
      maxN: Int,
      minSamples: Int,
      maxSamples: Int,
      seed: Long): List[SamplingScenario] = {

    // Get the scenarios eagerly to avoid carrying around the PRNG.
    // If there was stateless "randomness", this could be non-strict.
    val (scenarios, _) =
      (1 to trials).foldLeft((List.empty[SamplingScenario], new Random(seed))){
        case ((ss, r), _) =>
          val initSeed = r.nextLong()
          val samples = minSamples + r.nextInt(maxSamples - minSamples + 1)
          val n = r.nextInt(maxN + 1)
          val k = r.nextInt(n + 1)
          (SamplingScenario(initSeed, samples, n, k) :: ss, r)
      }

    scenarios
  }

  private def findFailures(trials: Int, maxN: Int, minSamples: Int, maxSamples: Int, seed: Long) = {
    samplingScenarios(trials, maxN, minSamples, maxSamples, seed).iterator.flatMap {
      case SamplingScenario(initSeed, samples, n, k) =>
        val dist = samplingDist(initSeed, samples, n, k)
        checkDistributionUniformity(samples, n, k, dist).toIterable
    }
  }

  private def allFailuresMsg[A](failures: Iterator[TestFailure[A]]): Option[String] = {
    if (failures.isEmpty)
      None
    else {
      val errorMsg =
        failures.foldLeft(""){ case (msg, TestFailure(samples, n, k, fails, dist)) =>
          val thisFail =
            s"For (n: $n, k: $k, samples: $samples), produced distribution: $dist. Failures:" +
              fails.mkString("\n\t", "\n\t", "\n\n")

          msg + thisFail
        }

      Option(errorMsg.trim)
    }
  }

  /**
    * For any `n` and `k`, `choose(n, k)` states should be sampled with uniform probability.
    * @param samples number of samples drawn
    * @param n number of objects from which to choose.
    * @param k number of elements drawn from the `n` objects.
    * @param dist distribution created from `samples` ''samples''.
    * @tparam A type of random variable.
    * @return a potential error.
    */
  private def checkDistributionUniformity[A](
      samples: Int,
      n: Int,
      k: Int,
      dist: Distribution[A]
  ): Option[TestFailure[A]] = {

    val expStates = choose(n, k)
    val expPr = 1d / expStates

    // This can be driven down by increasing `samples`.
    // This is proportional to 1 / sqrt(samples).
    // So, if samples is 10000, this is about 10%.
    val pctDiffFromExpPr = 100 * (10 / math.sqrt(samples))
    val delta = expPr * (pctDiffFromExpPr / 100)

    // Check that all states are sampled.
    val allStates =
      if (expStates == dist.size)
        List.empty[FailureReason]
      else List(MissingStates(expStates.toInt, dist.size))

    val errors =
      dist.foldRight(allStates){ case ((state, pr), errs) =>
        if (math.abs(expPr - pr) < delta)
          errs
        else WrongProbability(state, expPr, pr, delta) :: errs
      }

    if (errors.isEmpty)
      None
    else Option(TestFailure(samples, n, k, errors, dist))
  }

  private def samplingDist(initSeed: Long, samples: Int, n: Int, k: Int): Distribution[String] =
    drawSamples(initSeed, samples, n, k).mapValues { c => c / samples.toDouble }

  private def drawSamples(initSeed: Long, samples: Int, n: Int, k: Int): Map[String, NumSamples] = {
    val seed = initSeedScramble(initSeed)

    val samplesAndSeed =
      (1 to samples)
        .foldLeft((Map.empty[String, Int], seed)){ case ((m, s), _) =>

          // This is the sampling method whose uniformity is being tested.
          val (ind, newSeed) = sampleCombination(n, k, s)

          // It is important to sort here because order shouldn't matter since these
          // samples are to be treated as sets, not sequences.  By contract,
          // `sampleCombinations` doesn't guarantee a particular ordering but the
          // results are returned in an array (for computational efficiency).
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
  private type Distribution[A] = Map[A, Double]

  private case class SamplingScenario(initSeed: Long, samples: Int, n: Int, k: Int)

  private sealed trait FailureReason
  private final case class MissingStates(expected: Int, actual: Int) extends FailureReason {
    override def toString = s"States missing. Expected $expected states. Found $actual."
  }

  private final case class WrongProbability[A](state: A, expected: Double, actual: Double, delta: Double) extends FailureReason {
    override def toString = s"Incorrect probability for state $state. Expected pr = $expected +- $delta. Found pr = $actual."
  }
  private final case class TestFailure[A](samples: Int, n: Int, k: Int, failures: Seq[FailureReason], dist: Distribution[A])
}
