package com.eharmony.aloha.dataset.libsvm

import com.eharmony.aloha.dataset.libsvm.labeled.LibSvmLabelRowCreator
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class LibSvmLabelRowCreatorProducerTest extends LibSvmProducerTestBase {
    @Test def testCorrectFormatting(): Unit = {
        test(LibSvmLabelRowCreator.Producer(), "x")
    }
}
