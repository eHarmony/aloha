package com.eharmony.aloha.score.audit.take5;

import com.eharmony.aloha.id.ModelId;
import com.eharmony.aloha.score.audit.support.IntValue;
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
    public void testConstantModel() {
        final Object anyInput = "";
        final int expected = 1;
        final ModelId modelId = new ModelId(1, "one");
        final Option<Integer> constantValue = Option.apply(expected);

        final TreeAuditor<Object, Tree<? extends Value>> auditor = TreeAuditor.javaIntTreeAuditor();


//        final Auditor<Tree<? extends Value>, Object, Tree<IntValue>> auditor =
//                TreeAuditor.javaIntTreeAuditor();

//        final ConstantModel<Tree<? extends Value>, Integer, Tree<IntValue>> model =
//                new ConstantModel<>(modelId, constantValue, auditor);
//
//        final int actual = model.apply(anyInput).value().get().value();
//        assertEquals(expected, actual);
    }
}
