package com.eharmony.matching.featureSpecExtractor.vw

import org.junit.Assert._
import org.junit.{Test, Before}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwFeatureNormalizerTest {
    private[this] var normalizer: VwFeatureNormalizer = _

    @Before def setup() {
        normalizer = new VwFeatureNormalizer
    }

    @Test def testBlank() {
        assertEquals("", normalizer(""))
    }

    @Test def testSimple() {
        val vwLine: String = "1 1| |A a b c"
        assertEquals("1 1| |A:0.57735 a b c", normalizer.apply(vwLine).toString)
    }

    @Test def testMultipleNamespaces() {
        val vwLine: String = "1 1| |A a b c |b 1=2 3=4"
        assertEquals("1 1| |A:0.57735 a b c |b:0.70711 1=2 3=4", normalizer.apply(vwLine).toString)
    }

    @Test def testWithWeights() {
        val vwLine: String = "1 1| |A a:0.987 b c:0.435"
        assertEquals("1 1| |A:0.67988 a:0.987 b c:0.435", normalizer.apply(vwLine).toString)
    }
}
