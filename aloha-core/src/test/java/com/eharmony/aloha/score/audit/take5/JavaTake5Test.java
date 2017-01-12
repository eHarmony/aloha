package com.eharmony.aloha.score.audit.take5;

import com.eharmony.aloha.id.ModelId;
import com.eharmony.aloha.score.audit.support.IntValue;
import com.eharmony.aloha.score.audit.support.StringValue;
import com.eharmony.aloha.score.audit.support.Tree;
import com.eharmony.aloha.score.audit.support.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;

import static org.junit.Assert.assertEquals;

@RunWith(BlockJUnit4ClassRunner.class)
public class JavaTake5Test {
    @Test
    public void testStringConstantModel() {
        final Object anyInput = "";
        final String expected = "one";
        final ModelId modelId = new ModelId(1, "one");
        final Option<String> constantValue = Option.apply(expected);

        final TreeAuditor<String, StringValue> auditor = TreeAuditor.stringTreeAuditor();

        final ConstantModel<Tree<? extends Value>, String, Tree<StringValue>> model =
                ConstantModel.createFromJava(modelId, auditor, constantValue);

        final String actual = model.apply(anyInput).value().get().value();
        assertEquals(expected, actual);
    }

    @Test
    public void testIntConstantModel() {
        final Object anyInput = "";
        final int expected = 1;
        final ModelId modelId = new ModelId(1, "one");
        final Option<Integer> constantValue = Option.apply(expected);

        final TreeAuditor<Object, IntValue> auditor = TreeAuditor.intTreeAuditor();

        final ConstantModel<Tree<? extends Value>, Object, Tree<IntValue>> model =
                ConstantModel.createFromJava(modelId, auditor, constantValue);

        final int actual = model.apply(anyInput).value().get().value();
        assertEquals(expected, actual);
    }

    @Test
    public void testFloatModel() {
        final Object anyInput = "";
        final float expected = 1.23f;
        final ModelId modelId = new ModelId(1, "one");

        final OptionAuditor<Float> auditor = new OptionAuditor<>();
        final FloatModel<Option<?>, Option<Float>> model = new FloatModel<>(modelId, expected, auditor);
        final float actual = model.apply(anyInput).get();

        assertEquals(expected, actual, 0);
    }
}
