package com.eharmony.matching.aloha.models.conversion;

import com.eharmony.matching.aloha.factory.TypedModelFactory;
import com.eharmony.matching.aloha.models.Model;
import com.eharmony.aloha.score.Scores;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.util.Try;

import static com.eharmony.matching.aloha.models.conversion.DoubleToLongScalaTest.getJavaLongFactory;
import static com.eharmony.matching.aloha.models.conversion.DoubleToLongScalaTest.goodJson;
import static com.eharmony.matching.aloha.models.conversion.DoubleToLongScalaTest.stringInnerModelType;
import static com.eharmony.matching.aloha.models.conversion.DoubleToLongScalaTest.booleanInnerModelType;

import com.eharmony.matching.aloha.score.conversions.RelaxedConversions;
import spray.json.DeserializationException;


@RunWith(BlockJUnit4ClassRunner.class)
public class DoubleToLongJavaTest {

    @Test
    public void test() {
        final TypedModelFactory<Object,Long> factory = getJavaLongFactory();
        final String json = goodJson();
        final Try<Model<Object,Long>> modelTry = factory.fromString(json);

        final Model<Object, Long> m = modelTry.get();
        final Scores.Score s = m.score(null);

        assertEquals(1l, RelaxedConversions.asJavaLong(s).longValue());
        assertFalse(s.hasError());
        assertEquals(1, s.getSubScoresCount());
        assertEquals(1.00000001, RelaxedConversions.asJavaDouble(s.getSubScores(0)), 0);
        assertFalse(s.getSubScores(0).hasError());
        assertTrue(s.getSubScores(0).hasScore());
        assertEquals(0, s.getSubScores(0).getSubScoresCount());
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
