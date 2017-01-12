package com.eharmony.aloha.score.audit.take5;

import com.eharmony.aloha.id.ModelId;
import com.eharmony.aloha.score.audit.support.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.collection.immutable.Nil;
import scala.collection.immutable.Seq;

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

    @Test public void testHierarchicalModel() {
        final Object anyInput = "";
        final ModelId cId = new ModelId(1, "const");
        final int cValue = 1;

        final ConstantModel<Tree<? extends Value>, Object, Tree<IntValue>> constModel =
                ConstantModel.createFromJava(cId, TreeAuditor.intTreeAuditor(), Option.apply(cValue));

        final ModelId hId = new ModelId(2, "hier");
        final String hValue = "non-negative";

        final HierarchicalConstantModel<Tree<? extends Value>, String, Object, Tree<StringValue>> hierModel =
                HierarchicalConstantModel.createFromJava(hId, hValue, constModel, TreeAuditor.stringTreeAuditor());

        final Tree<StringValue> expected = hierarchicalOutput(cId, cValue, hId, hValue);
        final Tree<StringValue> actual = hierModel.apply(anyInput);
        assertEquals(expected, actual);
    }

    @Test
    public void testHierarchicalModelWithIntModel() {
        final Object anyInput = "";

        final ModelId cId = new ModelId(1, "const");
        final int cValue = 1;
        final IntModel<Tree<? extends Value>, Tree<IntValue>> constModel =
                IntModel.createFromJava(cId, cValue, TreeAuditor.intTreeAuditor());

        final ModelId hId = new ModelId(2, "hier");
        final String hValue = "non-negative";

        final HierarchicalConstantModel<Tree<? extends Value>, String, Object, Tree<StringValue>> hierModel =
                HierarchicalConstantModel.createFromJava(hId, hValue, constModel, TreeAuditor.stringTreeAuditor());

        final Tree<StringValue> expected = hierarchicalOutput(cId, cValue, hId, hValue);
        final Tree<StringValue> actual = hierModel.apply(anyInput);
        assertEquals(expected, actual);
    }

    private static Tree<StringValue> hierarchicalOutput(ModelId cId, int cValue, ModelId hId, String hValue) {
        final Seq<Tree<Value>> children =
                Nil.$colon$colon(Tree$.MODULE$.apply(cId, (Value) new IntValue(cValue))).toSeq();

        return Tree$.MODULE$.apply(hId, new StringValue(hValue), children);
    }

    @Test
    public void testIntModelOptionAuditor() {
        final Object anyInput = "";
        final int expected = 123;
        final ModelId modelId = new ModelId(1, "one");

        final OptionAuditor<Integer> auditor = new OptionAuditor<>();
        final IntModel<Option<?>, Option<Integer>> model =
                IntModel.createFromJava(modelId, expected, auditor);
        final int actual = model.apply(anyInput).get();

        assertEquals(expected, actual);
    }

    @Test
    public void testIntModelTreeAuditor() {
        final Object anyInput = "";
        final int expected = 123;
        final ModelId modelId = new ModelId(1, "one");

        final TreeAuditor<Object, IntValue> auditor = TreeAuditor.intTreeAuditor();
        final IntModel<Tree<? extends Value>, Tree<IntValue>> model =
                IntModel.createFromJava(modelId, expected, auditor);
        final int actual = model.apply(anyInput).value().get().value();

        assertEquals(expected, actual);
    }
}
