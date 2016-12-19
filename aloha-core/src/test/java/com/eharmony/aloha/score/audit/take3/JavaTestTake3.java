package com.eharmony.aloha.score.audit.take3;

import com.eharmony.aloha.id.ModelId;
import com.eharmony.aloha.id.ModelIdentity;
import deaktator.reflect.runtime.manifest.ManifestParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.reflect.Manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by ryan on 12/16/16.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaTestTake3 {
    @Test
    public void test1() {
        final Manifest<Integer> refInfoInt = manifest(Integer.class.getCanonicalName());
        final OptionAuditor<Integer> cmAud = new OptionAuditor<>(refInfoInt);

        final ConstantModel<OptionAuditor.OptionTC, Integer, Object, Option<Integer>> cModel =
            new ConstantModel<>(ModelId.empty(), 1, cmAud);

        final Float constant = 2f;
        final Manifest<Float> refInfoFloat = manifest(Float.class.getCanonicalName());
        final OptionAuditor<Float> aud = new OptionAuditor<>(refInfoFloat);

        final HierarchicalConstantModel<OptionAuditor.OptionTC, Integer, Float, Object, Option<Float>> hcm =
            HierarchicalConstantModel.createFromJava(ModelId.empty(), constant, aud, cModel);

        final Model<Object, Option<Float>> m = hcm;

        assertEquals(Option.apply(constant), m.apply(null));
    }

    @Test
    public void test2() {
        final Manifest<Integer> refInfoInt = manifest(Integer.class.getCanonicalName());
        final OptionAuditor<Integer> cmAud = new OptionAuditor<>(refInfoInt);

        final ConstantModel<OptionAuditor.OptionTC, Integer, Object, Option<Integer>> cModel =
            new ConstantModel<>(ModelId.empty(), 1, cmAud);

        final Float constant = 2f;
        final Manifest<Float> refInfoFloat = manifest(Float.class.getCanonicalName());
        final OptionAuditor<Float> aud = new OptionAuditor<>(refInfoFloat);

        final Model<Object, Option<Float>> hcm =
            HierarchicalConstantModel.createFromJava(ModelId.empty(), constant, aud, cModel);

        assertEquals(Option.apply(constant), hcm.apply(null));
    }

    @Test
    public void test3() {
        final Manifest<Integer> refInfoInt = manifest(Integer.class.getCanonicalName());
        final EitherAuditor<Integer> cmAud = new EitherAuditor<>(refInfoInt);

        final ConstantModel<EitherAuditor.EitherTC, Integer, Object, EitherAuditor.Result<ModelIdentity, Integer>> cModel =
                new ConstantModel<>(ModelId.empty(), 1, cmAud);

        final Float constant = 2f;
        final Manifest<Float> refInfoFloat = manifest(Float.class.getCanonicalName());
        final OptionAuditor<Float> aud = new OptionAuditor<>(refInfoFloat);

        // The following call gives the subsequent error (which is very good).
//      HierarchicalConstantModel.createFromJava(ModelId.empty(), constant, aud, cModel);

// [error] method createFromJava in class take3.HierarchicalConstantModel<T,SN,N,A,B> cannot be applied to given types;
// [error]   required: ModelIdentity, N, Auditor<ModelIdentity,T,N,B>, take3.AuditedModel<T,SN,A,SB>
// [error]   found: ModelId,
//                  Float,
//                  take3.OptionAuditor<Float>,
//                  take3.ConstantModel<
//                    take3.EitherAuditor.EitherTC,
//                    Integer,
//                    Object,
//                    take3.EitherAuditor.Result<ModelIdentity, Integer>>
// [error]   reason: inferred type does not conform to equality constraint(s)
// [error]     inferred: com.eharmony.aloha.score.audit.take3.EitherAuditor.EitherTC
// [error]     equality constraints(s): com.eharmony.aloha.score.audit.take3.EitherAuditor.EitherTC,
//                                      com.eharmony.aloha.score.audit.take3.OptionAuditor.OptionTC
// [error]                 HierarchicalConstantModel.createFromJava(ModelId.empty(), constant, aud, cModel);

//        assertEquals(Option.apply(constant), hcm.apply(null));
    }

    @Test
    public void test4() {
        final Manifest<Integer> refInfoInt = manifest(Integer.class.getCanonicalName());
        final EitherAuditor<Integer> cmAud = new EitherAuditor<>(refInfoInt);

        final ConstantModel<EitherAuditor.EitherTC, Integer, Object, EitherAuditor.Result<ModelIdentity, Integer>> cModel =
            new ConstantModel<>(new ModelId(1, "one"), 1, cmAud);

        final Float constant = 2f;
        final Manifest<Float> refInfoFloat = manifest(Float.class.getCanonicalName());
        final EitherAuditor<Float> aud = new EitherAuditor<>(refInfoFloat);

        final HierarchicalConstantModel<EitherAuditor.EitherTC, Integer, Float, Object, EitherAuditor.Result<ModelIdentity, Float>> hcm =
            HierarchicalConstantModel.createFromJava(new ModelId(2, "two"), constant, aud, cModel);

        final EitherAuditor.Result<ModelIdentity, Float> res = hcm.apply(null);
        if (res instanceof EitherAuditor.Success) {
            EitherAuditor.Success<ModelIdentity, Float> success = (EitherAuditor.Success<ModelIdentity, Float>) res;
            assertEquals(1, success.subValues().size());
            assertEquals(2f, success.valueToAudit(), 0);
        }
        else {
            fail();
        }
    }


    @SuppressWarnings("unchecked")
    private static <A> Manifest<A> manifest(final String strRep) {
        return (Manifest<A>) ManifestParser.parse(strRep).right().get();
    }
}
