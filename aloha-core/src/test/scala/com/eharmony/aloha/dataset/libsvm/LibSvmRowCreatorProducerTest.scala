package com.eharmony.aloha.dataset.libsvm

import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmRowCreator
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class LibSvmRowCreatorProducerTest extends LibSvmProducerTestBase {
    @Test def testCorrectFormatting() {
        test(LibSvmRowCreator.Producer())
    }
}
