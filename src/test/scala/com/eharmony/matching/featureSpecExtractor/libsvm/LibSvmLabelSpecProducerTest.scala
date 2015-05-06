package com.eharmony.matching.featureSpecExtractor.libsvm

import com.eharmony.matching.featureSpecExtractor.libsvm.labeled.LibSvmLabelSpecProducer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class LibSvmLabelSpecProducerTest extends LibSvmProducerTestBase {
    @Test def testCorrectFormatting(): Unit = {
        val seed = 0
        test(seed, new LibSvmLabelSpecProducer(seed), "x")
    }
}
