package com.eharmony.matching.aloha.factory;

import java.io.InputStream;
import java.io.StringReader;

import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.Arrays;

import com.eharmony.matching.aloha.models.conversion.DoubleToLongModel;
import scala.collection.JavaConversions;
import scala.collection.immutable.List;
import scala.util.Try;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import static org.junit.Assert.*;

import org.apache.commons.io.input.ReaderInputStream;

import com.fasterxml.classmate.GenericType;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

import com.eharmony.matching.aloha.interop.DoubleFactoryInfo;
import com.eharmony.matching.aloha.io.sources.InputStreamReadableSource;
import com.eharmony.matching.aloha.io.sources.ReaderReadableSource;
import com.eharmony.matching.aloha.io.sources.ReadableSourceConverters;
import com.eharmony.matching.aloha.io.sources.ReadableSource;
import com.eharmony.matching.aloha.io.sources.StringReadableSource;

import com.eharmony.matching.aloha.models.*;
import com.eharmony.matching.aloha.models.reg.RegressionModel;
import com.eharmony.matching.aloha.models.tree.decision.BasicDecisionTree;
import com.eharmony.matching.aloha.models.tree.decision.ModelDecisionTree;

import com.eharmony.aloha.score.Scores;
import com.eharmony.matching.aloha.util.ICList;




import com.eharmony.matching.aloha.score.conversions.StrictConversions;

/**
 * Just a few basic tests to ensure the proper functioning of the
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaDefaultModelFactoryTest {

    private static final String ERROR_MODEL_NAME = "gsaj";
    private static final long ERROR_MODEL_ID = 5625;
    private static final String ERROR_MODEL_MSG = "bad stuff happening";
    private static final String ERROR_MODEL_JSON = "{\"modelType\": \"Error\", \"modelId\": {\"id\": " + ERROR_MODEL_ID + ", \"name\": \"" + ERROR_MODEL_NAME + "\"}, \"errors\": [\"" + ERROR_MODEL_MSG + "\"]}";

    private static final String CONST_MODEL_NAME = "gaoas";
    private static final long CONST_MODEL_ID = 521;
    private static final double CONST_MODEL_VAL = 1234;
    private static final String CONST_MODEL_JSON = "{\"modelType\": \"Constant\", \"modelId\": {\"id\": " + CONST_MODEL_ID +", \"name\": \"" + CONST_MODEL_NAME + "\"}, \"value\": " + CONST_MODEL_VAL + "}";


    /**
     * Important that this is a list / array instead of a set because we want to ensure no collisions.
     */
    private static final String[] PARSER_NAMES;

    private static final TypedModelFactory<Map<String, Long>, Double> defaultFactory;

    static {
        String[] names = new String[] {
                ErrorModel.parser().modelType(),
                ConstantModel.parser().modelType(),
                CategoricalDistibutionModel.parser().modelType(),
                BasicDecisionTree.parser().modelType(),
                ModelDecisionTree.parser().modelType(),
                RegressionModel.parser().modelType(),
                SegmentationModel.parser().modelType(),
                DoubleToLongModel.parser().modelType(),
                ErrorSwallowingModel.parser().modelType()
        };

        Arrays.sort(names);
        PARSER_NAMES = names;

        final ModelFactory modelFactory = ModelFactory.defaultFactory();
        final ResolvedType resolvedType = new TypeResolver().resolve(new GenericType<Map<String, Long>>() {});
        final DoubleFactoryInfo<Map<String, Long>> mapDoubleFactoryInfo = new DoubleFactoryInfo<Map<String, Long>>(resolvedType);
        defaultFactory = modelFactory.toTypedFactory(mapDoubleFactoryInfo);
    }


    /**
     * Test the proper set of models are included in the default factory.
     */
    @Test
    public void testDefaultModelFactoryContainsCorrectModels() {
        final Collection<String> keys = JavaConversions.asJavaCollection(ModelFactory.defaultFactory().availableParsers().keys());
        final String[] names = keys.toArray(new String[keys.size()]);
        Arrays.sort(names);
        assertArrayEquals(PARSER_NAMES, names);
    }

    @Test
    public void testErrorModelFromDefaultFactory() {
        final Model<Map<String, Long>, Double> model = defaultFactory.fromString(ERROR_MODEL_JSON).get();

        assertEquals(ERROR_MODEL_NAME, model.modelId().getName());
        assertEquals(ERROR_MODEL_ID, model.modelId().getId());

        final Scores.Score score = model.score(null);

        assertFalse(score.hasScore());
        assertTrue(score.hasError());
        assertEquals(1, score.getError().getMessagesCount());
        assertEquals(ERROR_MODEL_MSG, score.getError().getMessages(0));
        assertEquals(0, score.getError().getMissingFeatures().getNamesCount());
    }

    @Test
    public void testConstantModelFromDefaultFactory() {
        final Model<Map<String, Long>, Double> model = defaultFactory.fromString(CONST_MODEL_JSON).get();

        assertEquals(CONST_MODEL_NAME, model.modelId().getName());
        assertEquals(CONST_MODEL_ID, model.modelId().getId());

        final Scores.Score score = model.score(null);
        final Double ds1 = StrictConversions.asJavaDouble(score);
        assertEquals(CONST_MODEL_VAL, ds1, 0);

        final Double ds2 = model.apply(null).get();
        assertEquals(CONST_MODEL_VAL, ds2, 0);
    }

    @Test
    public void testMultipleFromDefaultFactory() {
        final Model<Map<String, Long>, Double> constModel = defaultFactory.fromString(CONST_MODEL_JSON).get();
        final Model<Map<String, Long>, Double> errModel = defaultFactory.fromString(ERROR_MODEL_JSON).get();

        final ICList<ReadableSource> rtl =
                ICList.<ReadableSource>empty()
                .append(getInputStream(ERROR_MODEL_JSON), ReadableSourceConverters.inputStreamReadableConverter())
                .append(new InputStreamReadableSource(getInputStream(CONST_MODEL_JSON)))
                .append(getReader(ERROR_MODEL_JSON), ReadableSourceConverters.readerReadableConverter())
                .append(new ReaderReadableSource(getReader(CONST_MODEL_JSON)))
                .append(ERROR_MODEL_JSON, ReadableSourceConverters.stringToStringReadableConverter())
                .append(new StringReadableSource(CONST_MODEL_JSON));

        final List<ReadableSource> readables = rtl.toList();

        final List<Try<Model<Map<String,Long>,Double>>> tryList = defaultFactory.fromMultipleSources(readables);

        final Collection<Try<Model<Map<String, Long>, Double>>> tries = JavaConversions.asJavaCollection(tryList);

        int i = 0;
        for (Iterator<Try<Model<Map<String, Long>, Double>>> it = tries.iterator(); it.hasNext(); ++i) {
            final Model<Map<String, Long>, Double> m = it.next().get();
            final Model<Map<String, Long>, Double> exp = 0 == i % 2 ? errModel : constModel;
            assertEquals("on test " + i + ":", exp, m);
        }
    }

    private static InputStream getInputStream(String json) {
        return new ReaderInputStream(getReader(json));
    }

    private static StringReader getReader(String json) {
        return new StringReader(json);
    }
}
