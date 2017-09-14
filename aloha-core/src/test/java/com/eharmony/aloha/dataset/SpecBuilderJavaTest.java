package com.eharmony.aloha.dataset;

import com.eharmony.aloha.dataset.vw.VwParsingAndChainOfRespTest;
import com.eharmony.aloha.dataset.vw.cb.VwContextualBanditRowCreator;
import com.eharmony.aloha.dataset.vw.labeled.VwLabelRowCreator;
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator;
import com.eharmony.aloha.semantics.compiled.plugin.csv.CsvLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.util.Try;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;


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
        final List<? extends RowCreatorProducer<CsvLine, CharSequence, ? extends VwRowCreator<CsvLine>>> specProducers =
                Arrays.asList(new VwContextualBanditRowCreator.Producer<CsvLine>(), new VwLabelRowCreator.Producer<CsvLine>());

        final RowCreatorBuilder<CsvLine, CharSequence, VwRowCreator<CsvLine>> sb = RowCreatorBuilder.apply(
                VwParsingAndChainOfRespTest.semantics(),
                specProducers
        );

        final Try<VwRowCreator<CsvLine>> vwSpecTry = sb.fromResource(JSON_SPEC_LOC);

        // This would throw if it failed.
        final VwRowCreator<CsvLine> csvLineVwSpec = vwSpecTry.get();

        // This shouldn't be necessary but what the heck.
        assertNotNull(csvLineVwSpec);
    }
}
