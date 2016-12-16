package com.eharmony.aloha.score.audit.take2;

import com.eharmony.aloha.id.ModelId;
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
        final Manifest<Object> mO = manifest("java.lang.Object");
        final Manifest<Integer> mI = manifest("java.lang.Integer");
        final Manifest<Option<Integer>> mOI = manifest("scala.Option[java.lang.Integer]");

        final Either<String, Model<Object, Option<Integer>>> model =
            new Factory().createConstantModel(
                new Semantics<>(mO),      // semantics
                OptionTC.instance(),      // type constructor
                new OptionAuditor<>(mI),  // auditor
                1,               // constant
                mOI);                     // refInfo of model output type.

        model.right().get();
    }

    @Test
    public void test2() {
        final ModelId mId = ModelId.empty();
        final OptionAuditor<Float> audFloat =
                new OptionAuditor<>(JavaFactoryTest.<Float>manifest("java.lang.Float"));
        final OptionAuditor<Integer> audInt =
                new OptionAuditor<>(JavaFactoryTest.<Integer>manifest("java.lang.Integer"));

        // Doesn't compile: This is a huge compatibility failure!
        // final Model<Object, Option<Float>> cModel =
        //     new ConstantModel<>(ModelId.empty(), 5f, OptionTC.instance(), audFloat);

        // This is the default "introduce local variable" type in IntelliJ.  This is also a huge compatibility failure!
        // final ConstantModel<TypeCtor, Object, Float> cModel =
        //     new ConstantModel<>(ModelId.empty(), 5f, OptionTC.instance(), audFloat);

        // Necessary casting to get the proper model type
        // final Model<Object, Option<Float>> cModel =
        //     (Model<Object, Option<Float>>)
        //     (Model<Object, ?>)
        //     new ConstantModel<>(ModelId.empty(), 5f, OptionTC.instance(), audFloat);

        final Model<Object, Object> cModel =
            new ConstantModel<>(ModelId.empty(), 5f, OptionTC.instance(), audFloat);


        // Doesn't compile: This is a huge compatibility failure!
        // final HierarchicalConstantModel<TypeCtor, Object, Object, Option<Integer>> x =
        //     new HierarchicalConstantModel<>(ModelId.empty(), OptionTC.instance(), 1, audInt, cModel);

        // This is the default "introduce local variable" type in IntelliJ.  This is also a huge compatibility failure!
        // final HierarchicalConstantModel<TypeCtor, Object, Object, Integer> m =
        //     new HierarchicalConstantModel<>(ModelId.empty(), OptionTC.instance(), 1, audInt, cModel);

        final HierarchicalConstantModel<TypeCtor, Object, Object, Option<Integer>> hcm =
            (HierarchicalConstantModel<TypeCtor, Object, Object, Option<Integer>>)
            (HierarchicalConstantModel<TypeCtor, Object, Object, ?>)
            new HierarchicalConstantModel<>(ModelId.empty(), OptionTC.instance(), 1, audInt, cModel);

        final Model<Object, Option<Integer>> m = (Model<Object, Option<Integer>>) (Model<Object, ?>) hcm;
    }

    @SuppressWarnings("unchecked")
    private static <A> Manifest<A> manifest(final String strRep) {
        return (Manifest<A>) ManifestParser.parse(strRep).right().get();
    }
}
