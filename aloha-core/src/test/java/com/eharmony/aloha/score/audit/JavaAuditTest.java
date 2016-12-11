package com.eharmony.aloha.score.audit;

import com.eharmony.aloha.id.ModelId;
import deaktator.reflect.runtime.manifest.ManifestParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.reflect.Manifest;
import scala.util.Either;

import static org.junit.Assert.assertEquals;

/**
 * Created by ryan on 12/9/16.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaAuditTest {
    @Test
    public void test1() {
        final Model<Object, Option<Float>> m = getModel();
        final Float x = 1f;
        final Option<Float> value = m.apply(x);
        assertEquals(Option.apply(x), value);
    }

    private static Model<Object, Option<Float>> getModel() {
        final Manifest<Float> refInfo = manifest("java.lang.Float");
        final NoAudit<ModelId, Float> na = new NoAudit<ModelId, Float>(refInfo);

        final ModelFactory<Float, Option<Float>, NoAudit<ModelId, Float>> factory = ModelFactories.create(na);

        final Semantics<Object> semantics = new Semantics<Object>();
        final ModelId modelId = new ModelId(1, "test");
        final Either<String, Model<Object, Option<Float>>> modelAttempt =
                factory.createConstantModel(semantics, modelId, 1f);

        // This cast warning is a Scala bug.
        return (Model<Object, Option<Float>>) modelAttempt.right().get();
    }

    /**
     * Since the Manifest is generated at runtime, it
     * @param strRep a string representation of a Manifest.
     * @param <A> The type to which the value should be Manifest should be cast.
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <A> Manifest<A> manifest(final String strRep) {
        return (Manifest<A>) ManifestParser.parse(strRep).right().get();
    }
}
