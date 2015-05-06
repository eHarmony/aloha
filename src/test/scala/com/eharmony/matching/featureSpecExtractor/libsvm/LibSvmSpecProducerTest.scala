package com.eharmony.matching.featureSpecExtractor.libsvm

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.LibSvmSpecProducer

@RunWith(classOf[BlockJUnit4ClassRunner])
class LibSvmSpecProducerTest extends LibSvmProducerTestBase {
    @Test def testCorrectFormatting() {
        val seed = 0
        test(seed, new LibSvmSpecProducer(seed))
    }
}
