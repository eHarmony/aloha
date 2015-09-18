package com.eharmony.aloha.dataset.vw.cb

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.dataset.SparseFeatureExtractorFunction
import com.eharmony.aloha.dataset.json.SparseSpec
import com.eharmony.aloha.dataset.vw.cb.json.VwContextualBanditJson
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{Enum, CsvLines, CompiledSemanticsCsvPlugin, CsvLine}
import com.eharmony.aloha.semantics.func.GenFunc
import com.eharmony.aloha.util.StringSeq
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwContextualBanditRowCreatorTest {
    @Test def testMissingCbLabelInfoRemovesLabelOutput() {
        val fef = new SparseFeatureExtractorFunction(Vector("f1" -> GenFunc.f0("Empty", (_: Any) => Nil)))

        for {
            va <- Seq(None, Some(10L))
            vc <- Seq(None, Some(1d))
            vp <- Seq(None, Some(0.1))
            a = GenFunc.f0("action", (_: Any) => va)
            c = GenFunc.f0("cost", (_: Any) => vc)
            p = GenFunc.f0("prob", (_: Any) => vp)
            spec = VwContextualBanditRowCreator(fef, 0 to 0 toList, Nil, None, a, c, p)
            exp = (va, vc, vp) match {
                case (Some(av), Some(cv), Some(pv)) => s"$av:${VwRowCreator.LabelDecimalFormatter.format(cv)}:${VwRowCreator.LabelDecimalFormatter.format(pv)} |"
                case _ => ""
            }
        } assertEquals(exp, spec(())._2.toString)
    }


    @Test def testStringOptionClassLabels(): Unit = {
        import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvTypes._

        val semantics = CompiledSemantics(
            TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
            CompiledSemanticsCsvPlugin("action" -> StringOptionType),
            Seq.empty)

        val csvLines = CsvLines(Map("action" -> 0))

        val json =
             VwContextualBanditJson(
                imports = Vector("com.eharmony.aloha.feature.BasicFunctions._"),
                features = Vector(SparseSpec("b", """Seq(("", 1.0))"""),
                                  SparseSpec("c", """Seq(("", 1.0))""")),
                namespaces = None,
                normalizeFeatures = None,
                cbAction = "${action}",
                cbCost = "3", // 3 so it's not a class 1 or 2
                cbProbability = "0.5",
                classLabels = Option(StringSeq("Career", "Family")))

        val creator = new VwContextualBanditRowCreator.Producer[CsvLine].getRowCreator(semantics, json).get

      val (m1, y1) = creator(csvLines("Career"))
      assertEquals("1:3:0.5 | b c", y1.toString)

      val (m2, y2) = creator(csvLines("Family"))
      assertEquals("2:3:0.5 | b c", y2.toString)

      val (m3, y3) = creator(csvLines(""))
      assertEquals("| b c", y3.toString)
    }

  @Test def testEnumClassLabels(): Unit = {
    import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvTypes._

    val semantics = CompiledSemantics(
      TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
      CompiledSemanticsCsvPlugin("action" -> EnumType),
      Seq.empty)

    val csvLines = CsvLines(Map("action" -> 0), enums = Map("action" -> Enum.withNoNumbers("a.b.C", "Career", "Family")))

    val json =
      VwContextualBanditJson(
        imports = Vector("com.eharmony.aloha.feature.BasicFunctions._"),
        features = Vector(SparseSpec("b", """Seq(("", 1.0))"""),
          SparseSpec("c", """Seq(("", 1.0))""")),
        namespaces = None,
        normalizeFeatures = None,
        cbAction = "${action}",
        cbCost = "3", // 3 so it's not a class 1 or 2
        cbProbability = "0.5",
        classLabels = Option(StringSeq("Career", "Family")))

    val creator = new VwContextualBanditRowCreator.Producer[CsvLine].getRowCreator(semantics, json).get

    val (m1, y1) = creator(csvLines("Career"))
    assertEquals("1:3:0.5 | b c", y1.toString)

    val (m2, y2) = creator(csvLines("Family"))
    assertEquals("2:3:0.5 | b c", y2.toString)
  }
}
