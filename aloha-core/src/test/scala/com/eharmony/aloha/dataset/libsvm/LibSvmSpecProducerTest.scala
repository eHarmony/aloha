package com.eharmony.aloha.dataset.libsvm

import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmSpecProducer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class LibSvmSpecProducerTest extends LibSvmProducerTestBase {
    @Test def testCorrectFormatting() {
        val seed = 0
        test(seed, LibSvmSpecProducer())
    }
}
