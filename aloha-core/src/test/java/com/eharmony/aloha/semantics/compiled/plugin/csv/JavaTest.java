package com.eharmony.aloha.semantics.compiled.plugin.csv;

import com.eharmony.aloha.models.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;

import static org.junit.Assert.assertEquals;

@RunWith(BlockJUnit4ClassRunner.class)
public class JavaTest {
    @Test
    public void test1() {
        final Model<CsvLine, Option<Double>> model = new ExampleTest().getModel();
        final CsvLine line = ExampleTest.samples().apply(0);
        final Option<Double> score = model.apply(line);

        final double d = score.get();
        assertEquals(0.04125072271616186, d, 1e-6);
    }
}
