package com.eharmony.aloha.dataset.vw.cb

import com.eharmony.aloha.dataset.SparseFeatureExtractorFunction
import com.eharmony.aloha.dataset.vw.unlabeled.VwSpec
import com.eharmony.aloha.semantics.func.GenFunc
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.language.postfixOps

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwContextualBanditSpecTest {
    @Test def testMissingCbLabelInfoRemovesLabelOutput() {
        val fef = new SparseFeatureExtractorFunction(Vector("f1" -> GenFunc.f0("Empty", (_: Any) => Nil)))

        for {
            va <- Seq(None, Some(10L))
            vc <- Seq(None, Some(1d))
            vp <- Seq(None, Some(0.1))
            a = GenFunc.f0("action", (_: Any) => va)
            c = GenFunc.f0("cost", (_: Any) => vc)
            p = GenFunc.f0("prob", (_: Any) => vp)
            spec = VwContextualBanditSpec(fef, 0 to 0 toList, Nil, None, a, c, p)
            exp = (va, vc, vp) match {
                case (Some(av), Some(cv), Some(pv)) => s"$av:${VwSpec.LabelDecimalFormatter.format(cv)}:${VwSpec.LabelDecimalFormatter.format(pv)}|"
                case _ => ""
            }
        } assertEquals(exp, spec(())._2.toString)
    }
}
