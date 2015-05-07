package com.eharmony.matching.featureSpecExtractor.csv

import scala.concurrent.ExecutionContext.Implicits.global

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.Assert._

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CsvLine, CsvLines, CsvTypes, CompiledSemanticsCsvPlugin}
import com.eharmony.matching.featureSpecExtractor.{MissingAndErroneousFeatureInfo, SpecBuilder}

import scala.util.{Failure, Success}

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvSpecProducerTest {
    import CsvSpecProducerTest._

    @Test def testCompilationFailureWhenMissingDefault() {
        val json =
            """
              |{
              |  "imports": [],
              |  "features": [
              |    { "name": "f1", "spec": "${f1}" }
              |  ]
              |}
            """.stripMargin.trim

        specBuilder.fromString(json) match {
            case Success(s) => fail("Should have failed due to lack of default value.")
            case Failure(e) => assertEquals("No applicable producer found.  Given CsvSpecProducer", e.getMessage)
        }
    }

    @Test def testProperFormatting() {

        val json =
            """
              |{
              |  "imports": [],
              |  "features": [
              |    { "name": "f1", "spec": "${f1}", "defVal": 1 },
              |    { "name": "f2", "spec": "${f2}" }
              |  ]
              |}
            """.stripMargin.trim

        val expected = Seq(
            (MissingAndErroneousFeatureInfo(List("f1"), Nil),"1.0,2.0"),
            (MissingAndErroneousFeatureInfo(Nil, Nil),"3.0,4.0")
        )

        val spec = specBuilder.fromString(json).get

        val actual = lines.map(row => spec.toInput(row))

        assertEquals(expected, actual)
    }
}

object CsvSpecProducerTest {
    private lazy val (lines, specBuilder) = {
        val types = Seq("f1" -> CsvTypes.IntOptionType, "f2" -> CsvTypes.DoubleType)
        val plugin = CompiledSemanticsCsvPlugin(types.toMap)
        val semantics = CompiledSemantics(TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)), plugin, Nil)

        val csvLines = CsvLines(indices = types.unzip._1.zipWithIndex.toMap, fs = ",")

        val lines = csvLines(
            ",2.0",
            "3,4.0"
        )

        val sb = SpecBuilder(semantics, List(new CsvSpecProducer[CsvLine]()))
        (lines, sb)
    }
}
