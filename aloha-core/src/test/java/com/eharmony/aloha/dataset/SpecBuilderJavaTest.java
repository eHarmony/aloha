package com.eharmony.aloha.dataset;

import java.util.Arrays;
import java.util.List;

import com.eharmony.aloha.dataset.vw.VwParsingAndChainOfRespTest;
import com.eharmony.aloha.dataset.vw.cb.VwContextualBanditSpecProducer;
import com.eharmony.aloha.dataset.vw.labeled.VwLabelSpecProducer;
import com.eharmony.aloha.dataset.vw.unlabeled.VwSpec;
import scala.util.Try;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import static org.junit.Assert.assertNotNull;

import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvLine;
import com.eharmony.aloha.dataset.vw.VwParsingAndChainOfRespTest;
import com.eharmony.aloha.dataset.vw.cb.VwContextualBanditSpecProducer;
import com.eharmony.aloha.dataset.vw.labeled.VwLabelSpecProducer;
import com.eharmony.aloha.dataset.vw.unlabeled.VwSpec;


/**
 * The point of this test is to ensure that calling SpecBuilder from Java works with java Lists.
 * If the test doesn't throw, then it works.
 * @author R M Deak
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class SpecBuilderJavaTest {
    private static String JSON_SPEC_LOC = "com/eharmony/aloha/dataset/simpleCbSpec.json";

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
