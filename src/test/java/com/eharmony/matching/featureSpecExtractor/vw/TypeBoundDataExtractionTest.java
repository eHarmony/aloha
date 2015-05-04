package com.eharmony.matching.featureSpecExtractor.vw;

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics;
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.CsvLine;
import com.eharmony.matching.featureSpecExtractor.SpecBuilder;
import com.eharmony.matching.featureSpecExtractor.SpecProducer;
import com.eharmony.matching.featureSpecExtractor.vw.cb.VwContextualBanditSpecProducer;
import com.eharmony.matching.featureSpecExtractor.vw.labeled.VwLabelSpecProducer;
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.util.Try;

import java.util.Arrays;
import java.util.List;

@RunWith(BlockJUnit4ClassRunner.class)
public class TypeBoundDataExtractionTest {

    @Test
    public void test1() {

        final CompiledSemantics<CsvLine> semantics = VwParsingAndChainOfRespTest.semantics();

        // ArrayList<? extends SpecProducer<CsvLine, ? extends VwSpec<CsvLine>>> is the assigned type from IntelliJ.
        final List<? extends SpecProducer<CsvLine, ? extends VwSpec<CsvLine>>> producers =
                Arrays.asList(new VwContextualBanditSpecProducer<CsvLine>(), new VwLabelSpecProducer<CsvLine>());

//        final SpecBuilder<CsvLine, VwSpec<CsvLine>> sb = SpecBuilder.apply(semantics, producers);
//
//        final Try<VwSpec<CsvLine>> vwSpecTry = sb.fromClasspathResource("com/eharmony/matching/featureSpecExtractor/simpleCbSpec.json");
//
//        vwSpecTry.get();
    }
}
