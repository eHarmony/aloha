package com.eharmony.aloha.score.audit;

import com.eharmony.aloha.id.ModelId;
import com.eharmony.aloha.score.audit.JavaModelFactory.JavaModelFactoryException;
import deaktator.reflect.runtime.manifest.ManifestParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.reflect.Manifest;

import static org.junit.Assert.assertEquals;

@RunWith(BlockJUnit4ClassRunner.class)
public class JavaAuditTest {
    @Test
    public void test1() throws JavaModelFactoryException {
        final Float constant = 1f;
        final Model<Object, Option<Float>> m = getModel(constant);
        final Option<Float> value = m.apply(null);
        assertEquals(Option.apply(constant), value);
    }

    /**
     * Steps to creating a model in Java:
     * <ol>
     *     <li>Create an Aloha RefInfo <em>(scala.reflect.Manifest)</em>.</li>
     *     <li>Create a MorphableAuditor.</li>
     *     <li>Create JavaModelFactory using JavaModelFactories.create with the MorphableAuditor.</li>
     *     <li>Create or pass in Aloha Semantics</li>
     *     <li>Use JavaModelFactory with Semantics and a source to create the model.</li>
     * </ol>
     * @return a Model
     * @throws JavaModelFactoryException
     */
    private static Model<Object, Option<Float>> getModel(final Float constant) throws JavaModelFactoryException {
        final Manifest<Float> refInfo = manifest("java.lang.Float");
        final NoAudit<ModelId, Float> na = new NoAudit<ModelId, Float>(refInfo);
        final JavaModelFactory<Float, Option<Float>, NoAudit<ModelId, Float>> factory = JavaModelFactories.create(na);
        final Semantics<Object> semantics = new Semantics<Object>();
        final ModelId modelId = new ModelId(1, "test");
        return factory.createConstantModel(semantics, modelId, constant);
    }

    /**
     * Since the Manifest is generated at runtime, the type isn't known.  Therefore, we <b>MUST</b>
     * cast it.  Suppress the warning because it's annoying and we know it's correct in these tests.
     * @param strRep a string representation of a Manifest.
     * @param <A> The type to which the value should be Manifest should be cast.
     * @return a Manifest
     */
    @SuppressWarnings("unchecked")
    private static <A> Manifest<A> manifest(final String strRep) {
        return (Manifest<A>) ManifestParser.parse(strRep).right().get();
    }
}
