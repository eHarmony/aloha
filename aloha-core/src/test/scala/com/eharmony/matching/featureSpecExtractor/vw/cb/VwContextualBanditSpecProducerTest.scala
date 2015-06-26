package com.eharmony.matching.featureSpecExtractor.vw.cb

import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvLine
import com.eharmony.matching.featureSpecExtractor.SpecBuilder
import com.eharmony.matching.featureSpecExtractor.vw.VwParsingAndChainOfRespTest
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwContextualBanditSpecProducerTest {

    /**
     * Test when any of the three of the label components are missing, the label is omitted.  This makes a prediction
     * but omits input from training.
     */
    @Test def testAnyMissingDvFails(): Unit = {
        val semantics = VwParsingAndChainOfRespTest.semantics
        val sb = SpecBuilder(semantics, List(new VwContextualBanditSpecProducer[CsvLine]))
        val spec = sb.fromResource("com/eharmony/matching/featureSpecExtractor/simpleCbSpec.json").get

        val lines = VwParsingAndChainOfRespTest.csvLines(
            "Alex,,,,,,,2,1,0",
            "Bill,,,,,,,2,1,",
            "Carl,,,,,,,2,,0",
            "Dale,,,,,,,,1,0"
        )

        // TODO: Work on removing trailing and leading spaces.  This is clearly not perfect.
        val expected = Seq(
            "2:1:0|A name=Alex",
            "|A name=Bill",
            "|A name=Carl",
            "|A name=Dale"
        )

        (lines zip expected).zipWithIndex.foreach {
            case ((x, exp), i) =>
                val act = spec(x)._2.toString
                assertEquals(s"On test $i: ", exp, act)
            case d => fail(s"bad: $d")
        }
    }
}
