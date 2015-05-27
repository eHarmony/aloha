package com.eharmony.matching.featureSpecExtractor.vw.labeled

import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CsvLines, CsvLine}
import com.eharmony.matching.aloha.semantics.func.GenFunc.f0
import com.eharmony.matching.featureSpecExtractor.SparseFeatureExtractorFunction
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.language.implicitConversions

@RunWith(classOf[BlockJUnit4ClassRunner])
final class VwLabelSpecTest {

    private[this] val lab = 3d
    private[this] val imp0 = 0d
    private[this] val imp1 = 1d
    private[this] val imp2 = 2d
    private[this] val emptyTag = ""
    private[this] val tag = "t"

    private[this] implicit def liftToOption[A](a: A): Option[A] = Option(a)

    private[this] def spec(lab: Option[Double] = None, imp: Option[Double] = None, tag: Option[String] = None): VwLabelSpec[Any] = {
        val fef = new SparseFeatureExtractorFunction[Any](Vector("f1" -> f0("Empty", _ => Nil)))
        VwLabelSpec(fef, 0 to 0, Vector.empty, None, f0("", _ => lab), f0("", _ => imp), f0("", _ => tag))
    }

    private[this] def testLabelRemoval(spec: VwLabelSpec[Any], exp: String = " "): Unit = assertEquals(exp, spec(())._2.toString)

    // All of these should return empty label because the Label function returns a missing label.
    @Test def testS___() = testLabelRemoval(spec())
    @Test def testS__e() = testLabelRemoval(spec(tag = emptyTag))
    @Test def testS__t() = testLabelRemoval(spec(tag = tag))
    @Test def testS_0_() = testLabelRemoval(spec(imp = imp0))
    @Test def testS_0e() = testLabelRemoval(spec(imp = imp0, tag = emptyTag))
    @Test def testS_0t() = testLabelRemoval(spec(imp = imp0, tag = tag))
    @Test def testS_1_() = testLabelRemoval(spec(imp = imp1))
    @Test def testS_1e() = testLabelRemoval(spec(imp = imp1, tag = emptyTag))
    @Test def testS_1t() = testLabelRemoval(spec(imp = imp1, tag = tag))
    @Test def testS_2_() = testLabelRemoval(spec(imp = imp2))
    @Test def testS_2e() = testLabelRemoval(spec(imp = imp2, tag = emptyTag))
    @Test def testS_2t() = testLabelRemoval(spec(imp = imp2, tag = tag))

    // Importance not provided makes entire label vanish
    @Test def testS1_e() = testLabelRemoval(spec(lab = lab, tag = emptyTag))
    @Test def testS1_t() = testLabelRemoval(spec(lab = lab, tag = tag))

    // Importance of zero is given explicitly.
    @Test def testS10_() = testLabelRemoval(spec(lab = lab, imp = imp0), "3 0 | ")
    @Test def testS10e() = testLabelRemoval(spec(lab = lab, imp = imp0, tag = emptyTag), "3 0 | ")
    @Test def testS10t() = testLabelRemoval(spec(lab = lab, imp = imp0, tag = tag), "3 0 t| ")

    // Importance of 1 is omitted.
    @Test def testS11_() = testLabelRemoval(spec(lab = lab, imp = imp1), "3 | ")
    @Test def testS11e() = testLabelRemoval(spec(lab = lab, imp = imp1, tag = emptyTag), "3 | ")
    @Test def testS11t() = testLabelRemoval(spec(lab = lab, imp = imp1, tag = tag), "3 t| ")

    @Test def testS12_() = testLabelRemoval(spec(lab = lab, imp = imp2), "3 2 | ")
    @Test def testS12e() = testLabelRemoval(spec(lab = lab, imp = imp2, tag = emptyTag), "3 2 | ")
    @Test def testS12t() = testLabelRemoval(spec(lab = lab, imp = imp2, tag = tag), "3 2 t| ")


    @Test def testStringLabel() {
        val fef = new SparseFeatureExtractorFunction(Vector("f1" -> f0("Empty", (_: CsvLine) => Nil)))
        val spec = new VwLabelSpec(fef, 0 to 0, Vector.empty, None, f0("", (c: CsvLine) => c.od("lab")), f0("", (c: CsvLine) => Option(1d)), f0("", (c: CsvLine) => None))

        val csvLines = CsvLines(indices = Map("lab" -> 0))
        val lines = csvLines(
            "-1.0",
            "-0.99999999999999999",
            "-0.9999999999999999",
            "-0.0000000000000001",
            "-0.00000000000000001",
            "-0.000000000000000001",
             "0",
             "0.000000000000000001",
             "0.00000000000000001",
             "0.0000000000000001",
             "0.9999999999999999",
             "0.99999999999999999",
             "1.0"
        )

        val exp = Seq(
            "-1",
            "-1",
            "-0.9999999999999999",
            "-0.0000000000000001",
            "-0.00000000000000001",
            "-0",
            "0",
            "0",
            "0.00000000000000001",
            "0.0000000000000001",
            "0.9999999999999999",
            "1",
            "1"
        )

        lines.zip(exp) foreach { case(line, ex) => assertEquals(s"for line: ${line.line}", Option(ex), spec.stringLabel(line)) }
    }
}
