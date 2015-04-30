package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.io.StringReadable
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLines, CsvTypes}
import com.eharmony.matching.featureSpecExtractor.vw.labeled.VwLabelSpecProducer
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpecProducer
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[BlockJUnit4ClassRunner])
class SpecBuilderTest {
    @Test def test1(): Unit = {

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

        val lines = csvLines(
            "Ryan,1,2",
            "Jon,1,3",
            "Alice,,4",
            "Billy,0,"
        )

        val expected = Seq(
            "2 2| |A name=Ryan marriages=1",
            "3 3| |A name=Jon marriages=1",
            "4 4| |A name=Alice marriages=UNK",
            " | |A name=Billy marriages=0"
        )

        val json = StringReadable.fromResource("com/eharmony/matching/featureSpecExtractor/simpleSpec.json")

        val spec = SpecBuilder.build(semantics, json, Option(SpecType.VW), None, Seq(new VwLabelSpecProducer, new VwSpecProducer)).get

        val outs = lines.map(spec.toInput)

        // case(, i) =>
        outs.zip(expected).zipWithIndex.foreach{ case (((MissingAndErroneousFeatureInfo(missing, error), act), exp), i) =>
            assertEquals("for test $i: ", exp, act)
        }
    }
}
