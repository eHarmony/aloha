package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import java.text.DecimalFormat

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.feature.BasicFunctions
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import com.eharmony.matching.aloha.semantics.func.{GenFunc, GeneratedAccessor}
import com.eharmony.matching.featureSpecExtractor.SparseFeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpecTest._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Random, Try}

@RunWith(classOf[BlockJUnit4ClassRunner])
final class VwSpecTest {

    /**
     * Test that truncation ''ALWAYS'' happens close to one or negative one but that the truncated value is within
     * &epsilon; of the original value, where &epsilon; = 10^-''VwSpec.FeatureDecimalDigits''^ / 2.
     */
    @Test def testFeatureFormatterOneMinusEpsilonInterval() {
        val denom = math.pow(10, VwSpec.FeatureDecimalDigits - 1)
        val eps = math.pow(10, -VwSpec.FeatureDecimalDigits) / 2
        val r = new Random(0)

        val counts = 1 to 10000 flatMap { i =>
            val x = 1 - r.nextDouble() / denom
            Seq( x -> VwSpec.DecimalFormatter.format(x).toDouble,
                -x -> VwSpec.DecimalFormatter.format(-x).toDouble)
        } collect {
            case(d, s) if s == d => (d, s)                // If the values are equal, that's bad.
            case(d, s) if math.abs(s - d) > eps => (d, s) // If the values differ too much, that's bad.
        }

        assertEquals(s"counts size should be 0, not ${counts.size}.  Found ${counts.distinct.take(10)} ...", 0, counts.size)
    }

    /**
     * Test that truncation ''NEVER'' happens close to one or negative one.  Truncation only happens in the string only if
     * it happens in the double version.  For instance, for all ''D'', where
     *
     * 1 - 10^VwSpec.LabelDecimalDigits - 1^ < D < 1
     *
     * {{{
     * LabelDecimalFormatter.format(D).toDouble == D
     * }}}
     */
    @Test def testLabelFormatterOneMinusEpsilonInterval() {
        val denom = math.pow(10, VwSpec.LabelDecimalDigits - 1)
        val r = new Random(0)

        val counts = 1 to 10000 flatMap { i =>
            val x = 1 - r.nextDouble() / denom
            Seq( x -> VwSpec.LabelDecimalFormatter.format(x),
                -x -> VwSpec.LabelDecimalFormatter.format(-x))
        } collect { case(d, s) if s.toDouble != d => (d, s) }

        assertEquals(s"counts should be empty.  Found ${counts.distinct.take(10)} ...", 0, counts.size)
    }

    /**
     * Test that for all ''D'',
     * {{{
     * VwSpec.labelInEpsilonInterval(D) == (Set("-0", "0") contains VwSpec.LabelDecimalFormatter.format(D))
     * }}}
     */
    @Test def testLabelFormatterEpsilonInterval() {
        testEpsilonInterval(VwSpec.LabelDecimalDigits, VwSpec.labelInEpsilonInterval, VwSpec.LabelDecimalFormatter)
    }

    /**
     * Test that for all ''D'',
     * {{{
     * VwSpec.labelInEpsilonInterval(D) == (Set("-0", "0") contains VwSpec.LabelDecimalFormatter.format(D))
     * }}}
     */
    @Test def testFeatureFormatterEpsilonInterval() {
        testEpsilonInterval(VwSpec.FeatureDecimalDigits, VwSpec.inEpsilonInterval, VwSpec.DecimalFormatter)
    }

    private[this] def testEpsilonInterval(digits: Int, inEpsFunc: Double => Boolean, formatter: DecimalFormat) {
        val denom = math.pow(10, digits - 1)
        val r = new Random(0)
        val allowed = "^-?0$"

        val counts = 1 to 10000 map { i =>
            val x = (2 * r.nextDouble() - 1) / denom
            (inEpsFunc(x), formatter.format(x) matches allowed)
        } groupBy identity mapValues { _.size }

        assertEquals(s"for counts - $counts:  ", Seq((false, false), (true, true)), counts.keys.toSeq.sorted)
    }

    @Test def testFeatureNotInANamespaceFailsRequirement() {
        val nFeatures = 5
        for {
            nNamespaces <- 0 to nFeatures
            removal <-  Seq(DontDrop, DropDefault) ++ (0 until nNamespaces map Drop.apply)
            spec = createSpec(nFeatures, nNamespaces, removal)
        } {
            // If the number of namespaces equals the number of features, there are no features in the default
            // namespace so removing from the default should not cause a failure.  Otherwise, removing an item
            // from any namespace, including the default, should result in an error.
            if (nNamespaces == nFeatures && removal == DropDefault)
                assertTrue(s"#namespaces: $nNamespaces, removal: $removal: ", spec.isSuccess)
            else assertEquals(s"#namespaces: $nNamespaces, removal: $removal: ", removal == DontDrop, spec.isSuccess)
        }
    }

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

    sealed trait Removal
    case class Drop(ns: Int) extends Removal
    case object DropDefault extends Removal
    case object DontDrop extends Removal

    /**
     * Attempt to create a VwSpec.  This will fail when the constructor requirements are violated.
     * @param nFeatures number of features
     * @param nNamespaces number of namespaces (not including the default namespace)
     * @param removal how to remove an element from a namespace.
     * @return
     */
    def createSpec(nFeatures: Int, nNamespaces: Int, removal: Removal): Try[VwSpec[CsvLine]] = {

        val features = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).take(nFeatures).map{ c =>
            val name = s"a$c"
            name -> GenFunc.f1(GeneratedAccessor(name, (c: CsvLine) => c.oi(name)))("ind($" + name + ")", x => x.map(BasicFunctions.ind).getOrElse(Nil))
        }

        val (nssi, defi) = features.indices.splitAt(nNamespaces) match {
            case (nss, defNs) =>

                val nssi = nss.grouped(1).map { x =>
                    val i = x.head
                    removal match {
                        case Drop(n) if n == i => i.toString -> Vector.empty
                        case _                 => i.toString -> Vector(i)
                    }
                }.toIndexedSeq

                val defi = removal match {
                    case DropDefault if defNs.isEmpty => defNs
                    case DropDefault                  => defNs.tail
                    case _                            => defNs
                }

                (nssi, defi)
        }

        // This is intended to fail most of the time (to achieve the testing goal).
        val specTry = Try { new VwSpec(SparseFeatureExtractorFunction(features), defi, nssi, None) }
        specTry
    }
}
