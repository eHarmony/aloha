package com.eharmony.aloha.factory;

import java.io.InputStream;
import java.io.StringReader;

import java.util.*;

import com.eharmony.aloha.audit.MorphableAuditor;
import com.eharmony.aloha.audit.impl.TreeAuditor;
import com.eharmony.aloha.models.conversion.DoubleToLongModel;
import com.eharmony.aloha.models.exploration.BootstrapModel;
import com.eharmony.aloha.models.exploration.EpsilonGreedyModel;
import com.eharmony.aloha.reflect.RefInfo;
import com.eharmony.aloha.semantics.NoSemantics;
import com.eharmony.aloha.semantics.Semantics;
import scala.collection.JavaConversions;
import scala.collection.immutable.List;
import scala.reflect.Manifest;
import scala.util.Try;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import static org.junit.Assert.*;

import org.apache.commons.io.input.ReaderInputStream;

import com.eharmony.aloha.io.sources.InputStreamReadableSource;
import com.eharmony.aloha.io.sources.ReaderReadableSource;
import com.eharmony.aloha.io.sources.ReadableSourceConverters;
import com.eharmony.aloha.io.sources.ReadableSource;
import com.eharmony.aloha.io.sources.StringReadableSource;

import com.eharmony.aloha.models.*;
import com.eharmony.aloha.models.reg.RegressionModel;
import com.eharmony.aloha.models.tree.decision.BasicDecisionTree;
import com.eharmony.aloha.models.tree.decision.ModelDecisionTree;

import com.eharmony.aloha.util.ICList;


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

    private static final ModelFactory<TreeAuditor.Tree<?>, Double, Map<String, Long>, TreeAuditor.Tree<Double>> defaultFactory;

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
                ErrorSwallowingModel.parser().modelType(),
                EpsilonGreedyModel.parser().modelType(),
                BootstrapModel.parser().modelType()
        };

        Arrays.sort(names);
        PARSER_NAMES = names;

        final Manifest<Map<String, Long>> rIn =
                (Manifest<Map<String, Long>>) RefInfo.fromString("java.util.Map[java.lang.String, java.lang.Long]").right().get();
        final Manifest<Double> rOut = (Manifest<Double>) RefInfo.fromString("Double").right().get();
        final Semantics<Map<String, Long>> semantics = new NoSemantics<>(rIn);
        final MorphableAuditor<TreeAuditor.Tree<?>, Double, TreeAuditor.Tree<Double>> auditor = new TreeAuditor<>(false, false);
        defaultFactory = ModelFactory.defaultFactory(semantics, auditor, rOut);
    }


    /**
     * Test the proper set of models are included in the default factory.
     */
    @Test
    public void testDefaultModelFactoryContainsCorrectModels() {
        ArrayList<String> keys = new ArrayList<>();
        for (ModelParser parser: JavaConversions.asJavaCollection(defaultFactory.parsers())) {
            keys.add(parser.modelType());
        }
        final String[] names = keys.toArray(new String[keys.size()]);
        Arrays.sort(names);
        assertArrayEquals(PARSER_NAMES, names);
    }

    @Test
    public void testErrorModelFromDefaultFactory() {
        final Model<Map<String, Long>, TreeAuditor.Tree<Double>> model = defaultFactory.fromString(ERROR_MODEL_JSON).get();

        assertEquals(ERROR_MODEL_NAME, model.modelId().getName());
        assertEquals(ERROR_MODEL_ID, model.modelId().getId());

        final TreeAuditor.Tree<Double> score = model.apply(null);

        assertFalse(score.value().isDefined());
        assertEquals(1, score.errorMsgs().size());
        assertEquals(ERROR_MODEL_MSG, score.errorMsgs().apply(0));
        assertTrue(score.missingVarNames().isEmpty());
    }

    @Test
    public void testConstantModelFromDefaultFactory() {
        final Model<Map<String, Long>, TreeAuditor.Tree<Double>> model = defaultFactory.fromString(CONST_MODEL_JSON).get();

        assertEquals(CONST_MODEL_NAME, model.modelId().getName());
        assertEquals(CONST_MODEL_ID, model.modelId().getId());

        final TreeAuditor.Tree<Double> score = model.apply(null);
        final Double ds1 = score.value().get();
        assertEquals(CONST_MODEL_VAL, ds1, 0);

        final Double ds2 = model.apply(null).value().get();
        assertEquals(CONST_MODEL_VAL, ds2, 0);
    }

    @Test
    public void testMultipleFromDefaultFactory() {
        final Model<Map<String, Long>, TreeAuditor.Tree<Double>> constModel = defaultFactory.fromString(CONST_MODEL_JSON).get();
        final Model<Map<String, Long>, TreeAuditor.Tree<Double>> errModel = defaultFactory.fromString(ERROR_MODEL_JSON).get();

        final ICList<ReadableSource> rtl =
                ICList.<ReadableSource>empty()
                .append(getInputStream(ERROR_MODEL_JSON), ReadableSourceConverters.inputStreamReadableConverter())
                .append(new InputStreamReadableSource(getInputStream(CONST_MODEL_JSON)))
                .append(getReader(ERROR_MODEL_JSON), ReadableSourceConverters.readerReadableConverter())
                .append(new ReaderReadableSource(getReader(CONST_MODEL_JSON)))
                .append(ERROR_MODEL_JSON, ReadableSourceConverters.stringToStringReadableConverter())
                .append(new StringReadableSource(CONST_MODEL_JSON));

        final List<ReadableSource> readables = rtl.toList();

        final List<Try<Model<Map<String, Long>, TreeAuditor.Tree<Double>>>> tryList = defaultFactory.fromMultipleSources(readables);

        final Collection<Try<Model<Map<String, Long>, TreeAuditor.Tree<Double>>>> tries = JavaConversions.asJavaCollection(tryList);

        int i = 0;
        for (Iterator<Try<Model<Map<String, Long>, TreeAuditor.Tree<Double>>>> it = tries.iterator(); it.hasNext(); ++i) {
            final Model<Map<String, Long>, TreeAuditor.Tree<Double>> m = it.next().get();
            final Model<Map<String, Long>, TreeAuditor.Tree<Double>> exp = 0 == i % 2 ? errModel : constModel;
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
