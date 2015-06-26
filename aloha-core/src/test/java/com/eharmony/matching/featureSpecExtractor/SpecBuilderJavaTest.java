package com.eharmony.matching.featureSpecExtractor;

import java.util.Arrays;
import java.util.List;

import scala.util.Try;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import static org.junit.Assert.assertNotNull;

import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvLine;
import com.eharmony.matching.featureSpecExtractor.vw.VwParsingAndChainOfRespTest;
import com.eharmony.matching.featureSpecExtractor.vw.cb.VwContextualBanditSpecProducer;
import com.eharmony.matching.featureSpecExtractor.vw.labeled.VwLabelSpecProducer;
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec;


/**
 * The point of this test is to ensure that calling SpecBuilder from Java works with java Lists.
 * If the test doesn't throw, then it works.
 * @author R M Deak
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class SpecBuilderJavaTest {
    private static String JSON_SPEC_LOC = "com/eharmony/matching/featureSpecExtractor/simpleCbSpec.json";

    @Test
    public void testCallWithJavaList() {

        @SuppressWarnings("unchecked")
        final List<? extends SpecProducer<CsvLine, ? extends VwSpec<CsvLine>>> specProducers =
                Arrays.asList(new VwContextualBanditSpecProducer<CsvLine>(), new VwLabelSpecProducer<CsvLine>());

        final SpecBuilder<CsvLine, VwSpec<CsvLine>> sb = SpecBuilder.apply(
                VwParsingAndChainOfRespTest.semantics(),
                specProducers
        );

        final Try<VwSpec<CsvLine>> vwSpecTry = sb.fromResource(JSON_SPEC_LOC);

        // This would throw if it failed.
        final VwSpec<CsvLine> csvLineVwSpec = vwSpecTry.get();

        // This shouldn't be necessary but what the heck.
        assertNotNull(csvLineVwSpec);
    }
}
