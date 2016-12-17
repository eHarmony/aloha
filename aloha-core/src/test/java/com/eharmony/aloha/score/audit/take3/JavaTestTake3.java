package com.eharmony.aloha.score.audit.take3;

import com.eharmony.aloha.id.ModelId;
import deaktator.reflect.runtime.manifest.ManifestParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.reflect.Manifest;

import static org.junit.Assert.assertEquals;

/**
 * Created by ryan on 12/16/16.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaTestTake3 {
    @Test
    public void test1() {
        final Manifest<Integer> refInfoInt = manifest(Integer.class.getCanonicalName());
        final OptionAuditor<Integer> cmAud = new OptionAuditor<>(refInfoInt);

        final ConstantModel<TypeCtor, Integer, Object, Option<Integer>> cModel =
            new ConstantModel<>(ModelId.empty(), 1, cmAud);

        final Float constant = 2f;
        final Manifest<Float> refInfoFloat = manifest(Float.class.getCanonicalName());
        final OptionAuditor<Float> aud = new OptionAuditor<>(refInfoFloat);

        final HierarchicalConstantModel<TypeCtor, Object, Float, Object, Option<Float>> hcm =
            new HierarchicalConstantModel<>(ModelId.empty(), constant, cModel, aud);

        final Model<Object, Option<Float>> m = hcm;

        assertEquals(Option.apply(constant), m.apply(null));
    }

    @SuppressWarnings("unchecked")
    private static <A> Manifest<A> manifest(final String strRep) {
        return (Manifest<A>) ManifestParser.parse(strRep).right().get();
    }
}
