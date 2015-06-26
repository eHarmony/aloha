package com.eharmony.matching.aloha.models.ensemble.maxima

import java.{lang => jl}

import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.internal.runners.JUnit4ClassRunner

import com.eharmony.matching.aloha.id.ModelId
import com.eharmony.matching.aloha.models.ConstantModel
import com.eharmony.matching.aloha.models.ensemble.tie.TakeFirstTieBreaker
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.score.conversions.rich.RichScore
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput

@RunWith(classOf[JUnit4ClassRunner])
class ArgMaxModelTest {
    val digits = IndexedSeq("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

    @Test def test1() {
        val m = getArgMaxModelInt
        val s: Score = m.score(null)
        assertTrue("There should be no errors", s.allErrors.isEmpty)
        assertEquals("three", s.relaxed.asString.get)
    }

    /** Get an argMax model
      * @return [[com.eharmony.matching.aloha.models.ensemble.maxima.ArgMax]] [Any, Int, String].
      */
    def getArgMaxModelInt = getArgMaxModel[Int](Seq(3,1,4,2), _.toLong)

    def getArgMaxModelInteger: ArgMax[Any, Integer, String] = getArgMaxModel[jl.Integer](Seq(3,1,4,2), _.toLong)

    private[this] def getArgMaxModel[A: ScoreConverter: Ordering](vals: Seq[A], toLong: A => Long) = {
        val m = ArgMax(
            vals.map(i => ConstantModel(ModelOutput(i), ModelId(toLong(i), i.toString))),
            (1 to vals.size).map(digits),
            new TakeFirstTieBreaker[A],
            ModelId(0, "0")
        )
        m
    }

    /**
     * output should be 2.0.  b beats a and emits a 7.  3 beats 2 and emits a 6.  7 beats 6 so a 2.0 is emitted.
     * {{{
     * //                  +---------------------------------+
     * //   1 -----------> |2 --------> ("a")    (label: 3)  |
     * //     \            |  \                              | 7 (label 2.0)
     * //      \           |   -------> ("b")    (label: 7)  |
     * //       \          +---------------------------------+
     * //        \         +---------------------------------+
     * //         -------> | 5 --------> (2 byte) (label: 4) |
     * //                  |   \                             | 6 (label 5.0)
     * //                  |    -------> (3 byte) (label: 6) |
     * //                  +---------------------------------+
     * }}}
     */
    def getArgMaxTree = {

        // ArgMax[Any, String, java.lang.Integer]
        val a = ArgMax(
            Seq(ConstantModel(ModelOutput("a"), ModelId(3, "3")),
                ConstantModel(ModelOutput("b"), ModelId(7, "7"))),
            IndexedSeq(jl.Integer.valueOf(3), jl.Integer.valueOf(7)),
            new TakeFirstTieBreaker,
            ModelId(2, "2")
        )

        // Notice the
        // ArgMax[Any, Byte, java.lang.Integer]
        val b = ArgMax(
            Seq(ConstantModel(ModelOutput(2.toByte), ModelId(4, "4")),
                ConstantModel(ModelOutput(3.toByte), ModelId(6, "6"))),
            IndexedSeq(jl.Integer.valueOf(4), jl.Integer.valueOf(6)),
            new TakeFirstTieBreaker,
            ModelId(5, "5")
        )

        // ArgMax[Any, java.lang.Integer, java.lang.Double]
        val c = ArgMax(
            Seq(a, b),
            IndexedSeq(jl.Double.valueOf(2), jl.Double.valueOf(5)),
            new TakeFirstTieBreaker,
            ModelId(1, "1")
        )
        c
    }
}
