package com.eharmony.aloha.dataset

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmRowCreator
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import org.junit.Assert.{assertEquals, _}
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@RunWith(classOf[BlockJUnit4ClassRunner])
class RowCreatorBuilderTest {
    import RowCreatorBuilderTest._


    /**
     * This test is designed to ensure that the validation is being called.
     */
    @Test def testValidation() {
        val p = new LibSvmRowCreator.Producer[CsvLine]
        val semantics = CompiledSemantics(TwitterEvalCompiler(), CompiledSemanticsCsvPlugin(), Nil)
        val sb = RowCreatorBuilder(semantics, List(p))

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
            "|A name=Ryan marriages=1",
            "|A name=Jon marriages=1",
            "|A name=Alice marriages=UNK",
            "|A name=Billy marriages=0"
        )

        val sb = RowCreatorBuilder(semantics, List(new VwRowCreator.Producer[CsvLine]))
        val spec = sb.fromResource("com/eharmony/aloha/dataset/simpleSpec.json").get
        val outs = lines.map(spec.apply)

        // Test correctness.
        outs.zip(expected).zipWithIndex.foreach{ case (((MissingAndErroneousFeatureInfo(missing, error), act), exp), i) =>
            assertEquals(s"for test $i: ", exp, act.toString)
        }
    }
}

object RowCreatorBuilderTest {
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
