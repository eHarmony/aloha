package com.eharmony.aloha.dataset.libsvm;

import com.eharmony.aloha.dataset.libsvm.unlabeled.LibSvmSpecProducer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class LibSvmSpecProducerJavaTest {
    @Test
    public void testNoParamConstructor() {
        new LibSvmSpecProducer();
    }
}
