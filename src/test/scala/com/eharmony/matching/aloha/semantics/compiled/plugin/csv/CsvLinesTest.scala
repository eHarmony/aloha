package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import scala.util.Random
import scala.collection.GenTraversable

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

import com.eharmony.matching.aloha.util.Timing

/**
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvLinesTest extends Timing {

    /** Test that
      */
    @Test def testParallelVsSequential() {
        // In case of fluke or warm up issues.  Only need this many to succeed.
        val requiredSuccessRate = 0.9

        // Parameters (~2M = 20 * 1000 * 100 values)
        val trials = 20
        val samples = 1000
        val minSize = 50
        val maxSize = 150

        val top = maxSize - minSize + 1
        val rand = new Random(0)

        val colName = "intList"
        val reducer = reducingFunction(colName) _
        val lines = CsvLines(Map(colName -> 0))
        var times: List[Seq[Float]] = Nil

        val successes = (1 to trials).map { t => {
            // Create sequential and parallel versions of the raw string line data
            val sequential = (1 to samples).map(_ => Seq.fill(minSize + rand.nextInt(top))(rand.nextInt(1000)).mkString(","))
            val parallel = sequential.par

            // Create sequential and parallel versions of the CSV line objects.
            val seqLines = lines(sequential)
            val parLines = lines(parallel)

            // Test the parallel and sequential versions of the functions.
            val (seqSums, seqTime) = time(reducer(seqLines))
            val (parSums, parTime) = time(reducer(parLines))
            times = Seq(parTime, seqTime) :: times

            // Ensure the same result and that parallel version is faster.
            assertEquals(seqSums, parSums)

            parTime < seqTime
        }}.count(identity)

        println(s"parallel faster $successes / $trials")
        println(times.view.map(_.mkString(", ")).reverse.mkString("par (s), seq (s):\n", "\n", ""))
        assertTrue(requiredSuccessRate * trials <= successes)
    }

    private[this] def reducingFunction(colName: String)(lines: GenTraversable[CsvLine]): BigInt =
        lines.aggregate(BigInt(0))(_ + _.vi(colName).sum, _ + _)
}
