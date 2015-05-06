package com.eharmony.matching.featureSpecExtractor.libsvm;

import com.eharmony.matching.featureSpecExtractor.libsvm.unlabeled.LibSvmSpecProducer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class LibSvmSpecProducerJavaTest {
    @Test
    public void testNoParamConstructor() {
        new LibSvmSpecProducer();
    }

    @Test
    public void testOneParamConstructor() {
        new LibSvmSpecProducer(0);
    }
}
