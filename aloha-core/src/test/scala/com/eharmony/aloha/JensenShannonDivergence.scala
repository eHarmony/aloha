package com.eharmony.aloha

import cc.mallet.util.Maths.jensenShannonDivergence
import org.junit.Assert.assertTrue

object JensenShannonDivergence {

    def get(dist1: Array[Double], dist2: Array[Double]) = jensenShannonDivergence(dist1, dist2)

    /** Determine whether the Jensen-Shannon divergence is within a tolerance found to be good at determining if two
      * distributions are from the same.
      *
      * @param dist1
      * @param dist2
      * @param numDist1Samples
      * @param numDist2Samples
      * @return
      */
    def withinTolerance(dist1: Array[Double], dist2: Array[Double], numDist1Samples: Int, numDist2Samples: Int): Boolean =
        withinTolerance(dist1, dist2, math.min(numDist1Samples, numDist2Samples))

    def withinTolerance(dist1: Array[Double], dist2: Array[Double], samples: Int): Boolean = {
        val tol = tolerance(samples)
        val jsd = jensenShannonDivergence(dist1, dist2)
        jsd <= tol
    }

    def assertWithinTolerance(dist1: Array[Double], dist2: Array[Double], numDist1Samples: Int, numDist2Samples: Int) {
        assertWithinTolerance(dist1, dist2, math.min(numDist1Samples, numDist2Samples))
    }

    def assertWithinTolerance(dist1: Array[Double], dist2: Array[Double], samples: Int) {
        val tol = tolerance(samples)
        val jsd = jensenShannonDivergence(dist1, dist2)
        assertTrue(getErrorMessage(dist1, dist2, samples, jsd, tol), jsd <= tol)
    }

    /** This formula comes from the analysis by R Deak on the wiki
      * https://wiki.corp.eharmony.com/display/MAT/Sampling+from+a+Discrete+Distribution
      *
      * If we look at many random samples from many randomly constructed distributions,
      * we can see that the there is a powerlaw relationship between the Jensen-Shannon
      * Divergence and the number of samples drawn from the distribution.  This is that
      * relationship that was found (for distributions from 2 to 11 distinct values).
      * @param numSamples
      * @return
      */
    def tolerance(numSamples: Int): Double = 6.0184 * math.pow(numSamples, -1.008)

    /** Formulate the error message if there is one.
      * @param dist
      * @param emp
      * @param samples
      * @param js
      * @param maxJs
      * @return
      */
    def getErrorMessage(dist: Array[Double], emp: Array[Double], samples: Int, js: Double, maxJs: Double): String = {
        new StringBuilder().append("For ").
            append(samples).
            append(" samples. distribution ").
            append(dist.mkString("{", ",", "}")).
            append(" doesn't match distribution ").
            append(emp.mkString("{", ",", "}")).
            append(". Expected a Jensen-Shannon Divergence under ").
            append(maxJs).
            append(" but found values of ").
            append(js).
            toString()
    }
}
