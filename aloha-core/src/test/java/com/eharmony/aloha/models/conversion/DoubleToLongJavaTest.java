package com.eharmony.aloha.models.conversion;

import com.eharmony.aloha.audit.impl.tree.RootedTree;
import com.eharmony.aloha.audit.impl.tree.Tree;
import com.eharmony.aloha.factory.ModelFactory;
import com.eharmony.aloha.models.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.util.Try;
import spray.json.DeserializationException;

import static com.eharmony.aloha.models.conversion.DoubleToLongModelTest.*;
import static org.junit.Assert.*;


@RunWith(BlockJUnit4ClassRunner.class)
public class DoubleToLongJavaTest {

    @Test
    public void test() {
        final ModelFactory<Object, RootedTree<Object, Long>> factory = getJavaLongFactory();
        final String json = goodJson();
        final Try<Model<Object, RootedTree<Object, Long>>> modelTry = factory.fromString(json);

        final Model<Object, RootedTree<Object, Long>> m = modelTry.get();
        final RootedTree<Object, Long> s = m.apply(null);

        final Tree<Object> sub = s.subvalues().head();

        assertEquals(1l, s.value().get().longValue());
        assertTrue(s.errorMsgs().isEmpty() && s.missingVarNames().isEmpty());
        assertEquals(1, s.subvalues().size());
        assertEquals(1.00000001, (double) sub.value().get(), 0);
        assertTrue(sub.errorMsgs().isEmpty() && sub.missingVarNames().isEmpty());
        assertTrue(sub.value().isDefined());
        assertTrue(sub.subvalues().isEmpty());
    }

    @Test
    public void testStringInnerModel() {
        try {
            getJavaLongFactory().fromString(stringInnerModelType()).get();
            fail("Should have failed");
        }
        catch (DeserializationException e) {
            assertEquals("Expected Double as JsNumber, but got \"1.00000001\"", e.getMessage());
        }
        catch (Exception e) {
            fail("Expected DeserializationException");
        }
    }

    @Test
    public void testBooleanInnerModel() {
        try {
            getJavaLongFactory().fromString(booleanInnerModelType()).get();
            fail("Should have failed");
        }
        catch (DeserializationException e) {
            assertEquals("Expected Double as JsNumber, but got true", e.getMessage());
        }
        catch (Exception e) {
            fail("Expected DeserializationException");
        }
    }
}
