package com.eharmony.aloha.score.audit.take2;

import com.eharmony.aloha.score.audit.*;
import deaktator.reflect.runtime.manifest.ManifestParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.reflect.Manifest;
import scala.util.Either;

/**
 * Created by ryan on 12/15/16.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaFactoryTest {
    @Test
    public void test1() {
        @SuppressWarnings("unchecked") final Manifest<Object> mO =
                (Manifest<Object>) ManifestParser.parse("java.lang.Object").right().get();
        @SuppressWarnings("unchecked") final Manifest<Integer> mI =
                (Manifest<Integer>) ManifestParser.parse("java.lang.Integer").right().get();
        @SuppressWarnings("unchecked") final Manifest<Option<Integer>> mOI =
                (Manifest<Option<Integer>>) ManifestParser.parse("scala.Option[java.lang.Integer]").right().get();

        final Either<String, Model<Object, Option<Integer>>> model =
            new Factory().createConstantModel(
                new Semantics<>(mO),      // semantics
                OptionTC.instance(),      // type constructor
                new OptionAuditor<>(mI),  // auditor
                1,               // constant
                mOI);                     // refInfo of model output type.

        model.right().get();
    }
}
