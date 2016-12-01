package com.eharmony.aloha.semantics.compiled.plugin.csv;

import com.eharmony.aloha.models.Model;
import com.eharmony.aloha.score.Scores;
import com.eharmony.aloha.score.conversions.RelaxedConversions;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class JavaTest {
    @Test
    public void test1() {
        final Model<CsvLine, Object> model = new ExampleTest().getModel();
        final CsvLine line = ExampleTest.samples().apply(0);
        final Scores.Score score = model.score(line);

        final double d = RelaxedConversions.asJavaDouble(score);
        assertEquals(0.04125072271616186, d, 1e-6);
    }
}
