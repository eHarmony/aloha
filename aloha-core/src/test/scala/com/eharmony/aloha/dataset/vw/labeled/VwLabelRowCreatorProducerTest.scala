package com.eharmony.aloha.dataset.vw.labeled

import com.eharmony.aloha.dataset.RowCreatorBuilder
import com.eharmony.aloha.dataset.vw.VwParsingAndChainOfRespTest
import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvLine
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwLabelRowCreatorProducerTest {

    @Test def testNonDefaultTagThatsMissingDoesntRemoveLabel() {
        val semantics = VwParsingAndChainOfRespTest.semantics

        val sb = RowCreatorBuilder(semantics, List(new VwLabelRowCreator.Producer[CsvLine]))
        val spec = sb.fromResource("com/eharmony/aloha/dataset/simpleSpecWithTag.json").get

        val lines = VwParsingAndChainOfRespTest.csvLines(
            "Alex,,1,,2,,,,,",
            "Bill,,2,,3,,,,,",
            "Carl,,0,,,,,,,",
            "Dale,,3,,1,,,,,"
        )

        val expected = Seq(
            "1 2|A name=Alex marriages=UNK",
            "2 3|A name=Bill marriages=UNK",
            "0 |A name=Carl marriages=UNK",
            "3 1|A name=Dale marriages=UNK"
        )

        lines.zip(expected).foreach{
            case(x, exp) => assertEquals(
                s"for ${x.line}: ",
                exp,
                spec(x)._2.toString
            )
        }
    }


    @Test def testImportanceMissingRemovesLabel() {
        val semantics = VwParsingAndChainOfRespTest.semantics

        val sb = RowCreatorBuilder(semantics, List(new VwLabelRowCreator.Producer[CsvLine]))
        val spec = sb.fromResource("com/eharmony/aloha/dataset/simpleSpecWithImp.json").get

        val lines = VwParsingAndChainOfRespTest.csvLines(
            "Alex,,1,,2,,,,,",
            "Bill,,2,,3,,,,,",
            "Carl,,0,,,,,,,",
            "Dale,,3,,1,,,,,"
        )

        val expected = Seq(
            "1 2 1|A name=Alex marriages=UNK",
            "2 3 2|A name=Bill marriages=UNK",
            "|A name=Carl marriages=UNK",  // Omitting the importance variable removes the entire label.
            "3 3|A name=Dale marriages=UNK"
        )

        lines.zip(expected).foreach{
            case(x, exp) => assertEquals(
                s"for ${x.line}: ",
                exp,
                spec(x)._2.toString
            )
        }
    }

    @Test def testLabelMissingRemovesLabel() {

        val semantics = VwParsingAndChainOfRespTest.semantics

        val sb = RowCreatorBuilder(semantics, List(new VwLabelRowCreator.Producer[CsvLine]))
        val spec = sb.fromResource("com/eharmony/aloha/dataset/simpleSpec.json").get

        val lines = VwParsingAndChainOfRespTest.csvLines(
            "Alex,,1,,,,,,,",
            "Bill,,2,,,,,,,",
            "Carl,,,,,,,,,"
        )

        val expected = Seq(
            "1 1|A name=Alex marriages=UNK",
            "2 2|A name=Bill marriages=UNK",
            "|A name=Carl marriages=UNK"
        )

        lines.zip(expected).foreach{ case(x, exp) => assertEquals(s"for ${x.line}: ", exp, spec(x)._2.toString) }
    }
}
