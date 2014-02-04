package com.eharmony.matching.aloha.semantics.compiled.plugin.csv;

import com.eharmony.matching.aloha.models.Model;
import com.eharmony.matching.aloha.score.Scores;
import com.eharmony.matching.aloha.score.conversions.RelaxedConversions;
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
        System.out.println("my value: " + d);
    }
}
