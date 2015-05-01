package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpecProducer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[BlockJUnit4ClassRunner])
class SpecBuilderTest {
    import SpecBuilderTest._

    @Test def test1(): Unit = {
        val lines = csvLines(
            "Ryan,1,2",
            "Jon,1,3",
            "Alice,,4",
            "Billy,0,"
        )

        val expected = Seq(
            " |A name=Ryan marriages=1",
            " |A name=Jon marriages=1",
            " |A name=Alice marriages=UNK",
            " |A name=Billy marriages=0"
        )

        val sb = SpecBuilder(semantics, Seq(new VwSpecProducer[CsvLine]))
        val spec = sb.fromResource("com/eharmony/matching/featureSpecExtractor/simpleSpec.json").get
        val outs = lines.map(spec.toInput)

        // Test correctness.
        outs.zip(expected).zipWithIndex.foreach{ case (((MissingAndErroneousFeatureInfo(missing, error), act), exp), i) =>
            assertEquals("for test $i: ", exp, act)
        }
    }
}

object SpecBuilderTest {
    val (semantics, csvLines) = {

        val features = Seq(
            "profile.first_name" -> CsvTypes.StringOptionType,
            "profile.number_of_marriages" -> CsvTypes.IntOptionType,
            "profile.user_id" -> CsvTypes.LongOptionType
        )

        val csvPlugin = CompiledSemanticsCsvPlugin(features.toMap)

        val semantics = CompiledSemantics(
            TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
            csvPlugin,
            Seq.empty)

        val csvLines = CsvLines(
            indices = features.unzip._1.zipWithIndex.toMap,
            fs = ","
        )

        (semantics, csvLines)
    }
}
