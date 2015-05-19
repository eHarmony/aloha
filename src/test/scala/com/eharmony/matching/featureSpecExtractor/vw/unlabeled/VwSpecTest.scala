package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CsvLines, CsvLine, CsvTypes, CompiledSemanticsCsvPlugin}
import com.eharmony.matching.aloha.semantics.func.{GeneratedAccessor, GenFunc}
import com.eharmony.matching.featureSpecExtractor.SparseFeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpecTest.{Precision, csvLines}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwSpecTest {

    /**
     *
     1
     1 Test that we have at most 6 digits in the mantissa but that the value is not zero-padded.
     */
    @Test def testNamespaces() {

        val lines = csvLines("1,2", "2,3", "3,4", "3,5" )

        val expected = Seq(
            " |addition i_plus_d:3 |division d_div_i:2",
            " |addition i_plus_d:5 |division d_div_i:1.5",
            s" |addition i_plus_d:7 |division d_div_i:${((4d / 3) * Precision).round / Precision}",
            s" |addition i_plus_d:8 |division d_div_i:${((5d / 3) * Precision).round / Precision}"
        )

        val features = SparseFeatureExtractorFunction(Vector(
            "i_plus_d" -> GenFunc.f2(
                GeneratedAccessor("i", (_: CsvLine).i("i")),
                GeneratedAccessor("d", (_: CsvLine).d("d")))(
                    "${i} + ${d}",
                    (i, d) => Seq(("", i + d))
                ),
            "d_div_i" -> GenFunc.f2(
                GeneratedAccessor("i", (_: CsvLine).i("i")),
                GeneratedAccessor("d", (_: CsvLine).d("d")))(
                    "${d} / ${i}",
                    (i, d) => Seq(("", d / i))
                )
        ))

        val spec = new VwSpec[CsvLine](
            features,
            Vector.empty,                                             // default namespace indices
            Vector("addition" -> Vector(0), "division" -> Vector(1)), // namespaces
            None,                                                     // no normalizer
            false                                                     // include zeroes
        )

        lines.zip(expected) foreach { case(line, exp) =>
            assertEquals(s"for includeZeroValues=${line.line}, ", exp, spec(line)._2.toString)
        }
    }

    @Test def testIncZeroFalse() {
        val expected = Map(
            false -> Seq(" ", " i_plus_d", " i_plus_d:2"),
            true -> Seq(" i_plus_d:0", " i_plus_d", " i_plus_d:2")
        )

        val lines = csvLines("0,0", "0,1", "0,2")

        val features = SparseFeatureExtractorFunction(Vector(
            "i_plus_d" -> GenFunc.f2(
                GeneratedAccessor("i", (_: CsvLine).i("i")),
                GeneratedAccessor("d", (_: CsvLine).d("d")))(
                    "${i} + ${d}",
                    (i, d) => Seq(("", i + d))
                )
        ))

        expected foreach { case (includeZeroes, exp) =>
            val spec = new VwSpec[CsvLine](
                features,
                Vector(0),    // default namespace indices
                Vector.empty, // namespaces
                None,         // no normalizer
                includeZeroes // include zeroes
            )

            assertEquals(s"for includeZeroValues=$includeZeroes", exp, lines.map(c => spec(c)._2.toString))
        }
    }
}

private object VwSpecTest {
    val Precision = 1.0e6

    lazy val semantics = {
        val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))
        val plugin = CompiledSemanticsCsvPlugin(
            "i" -> CsvTypes.IntType,
            "d" -> CsvTypes.DoubleType
        )
        CompiledSemantics[CsvLine](compiler, plugin, Nil)
    }

    lazy val csvLines = CsvLines(
        indices = Map("i" -> 0, "d" -> 1),
        fs = ","
    )
}
