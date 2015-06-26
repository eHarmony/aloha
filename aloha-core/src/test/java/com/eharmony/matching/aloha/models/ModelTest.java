package com.eharmony.matching.aloha.models;

import com.eharmony.matching.aloha.id.ModelId$;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterable;
import scala.collection.Iterable$;
import scala.collection.Seq;
import scala.collection.immutable.List$;
import scala.util.Either;
import scala.util.Left;

import scala.Tuple2$;

import org.junit.Test;
import static org.junit.Assert.*;

import com.eharmony.matching.aloha.id.ModelId;
import com.eharmony.matching.aloha.id.ModelIdentity;
import com.eharmony.aloha.score.Scores;
import scala.util.Left$;

import java.io.IOException;

@RunWith(BlockJUnit4ClassRunner.class)
public class ModelTest {

    @Test
    public void testFailModel() {
        final FailModel<String, Long> model = new FailModel<String, Long>();

        // Model ID
        assertEquals(ModelId$.MODULE$.apply(1, "Fail"), model.modelId());

        // Option
        assertEquals(Option.<Long>empty(), model.apply(null));

        // Either
        final Either<Tuple2<Seq<String>, Iterable<String>>, Long> e = model.scoreAsEither(null);
        assertTrue(e.isLeft());
        assertEquals(0, e.left().get()._1().size());
        assertEquals(0, e.left().get()._2().size());

        // score
        assertNotNull(model.score(null));
        assertEquals("com.eharmony.aloha.score.Scores.Score", model.score(null).getClass().getCanonicalName());

        // getScore
        try {
            model.getScore(null, false);
            fail();
        }
        catch (UnsupportedOperationException ex) {}
    }

    private static class FailModel<A, B> implements Model<A, B> {

        @Override
        public ModelIdentity modelId() {
            return ModelId$.MODULE$.apply(1, "Fail");
        }

        @Override
        public Option<B> apply(A a) {
            return Option.empty();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Either<Tuple2<Seq<String>, Iterable<String>>, B> scoreAsEither(A a) {
            final Seq<String> empty = List$.MODULE$.empty();
            final Iterable<String> empty1 = (Iterable<String>) Iterable$.MODULE$.empty();
            return Left$.MODULE$.apply(Tuple2$.MODULE$.apply(empty, empty1));
        }

        @Override
        public Scores.Score score(A a) {
            return Scores.Score.getDefaultInstance();
        }

        @Override
        public Tuple2<Either<Tuple2<Seq<String>, Iterable<String>>, B>, Option<Scores.Score>> getScore(A a, boolean audit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {}
    }
}
