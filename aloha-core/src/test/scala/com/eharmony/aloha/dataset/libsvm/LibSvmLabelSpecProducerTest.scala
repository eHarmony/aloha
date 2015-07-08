package com.eharmony.aloha.dataset.libsvm

import com.eharmony.aloha.dataset.libsvm.labeled.LibSvmLabelSpecProducer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class LibSvmLabelSpecProducerTest extends LibSvmProducerTestBase {
    @Test def testCorrectFormatting(): Unit = {
        test(LibSvmLabelSpecProducer(), "x")
    }
}
