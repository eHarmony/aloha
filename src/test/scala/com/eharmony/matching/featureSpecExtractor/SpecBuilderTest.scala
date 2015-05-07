package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.LibSvmSpecProducer
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpecProducer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@RunWith(classOf[BlockJUnit4ClassRunner])
class SpecBuilderTest {
    import SpecBuilderTest._


    /**
     * This test is designed to ensure that the validation is being called.
     */
    @Test def testValidation() {
        val p = new LibSvmSpecProducer[CsvLine]
        val semantics = CompiledSemantics(TwitterEvalCompiler(), CompiledSemanticsCsvPlugin(), Nil)
        val sb = SpecBuilder(semantics, List(p))

        sb.fromString(getRepeatedJson(1)) match {
            case Failure(e) => throw e
            case Success(s) =>  // continue as planned.
        }

        sb.fromString(getRepeatedJson(2)) match {
            case Success(s) => fail("Should fail due to validation error.")
            case Failure(e) => assertEquals(s"No applicable producer found.  Given ${p.getClass.getSimpleName}", e.getMessage)
        }
    }

    private[this] def getRepeatedJson(cnt: Int) = {
        val spec = """{"name":"sameName", "spec":"Seq((\"\", 1.0))"}"""
        val specs = List.fill(cnt)(spec).mkString(", ")

        s"""
           |{
           |  "imports":[],
           |  "features":[ $specs ]
           |}
         """.stripMargin.trim
    }

    @Test def testVwCorrectness(): Unit = {
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

        val sb = SpecBuilder(semantics, List(new VwSpecProducer[CsvLine]))
        val spec = sb.fromResource("com/eharmony/matching/featureSpecExtractor/simpleSpec.json").get
        val outs = lines.map(spec.apply)

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
