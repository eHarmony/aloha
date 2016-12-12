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
    public void testHierarchical() {
        final Double cI = 1d;
        final ModelId idI = new ModelId(1, "one");
        final Float cO = 2f;
        final ModelId idO = new ModelId(2, "two");

        final Manifest<Float> mO = manifest("java.lang.Float");
        final OptionAuditor<ModelId, Float> aO = new OptionAuditor<>(mO);
        final Manifest<Double> mI = manifest("java.lang.Double");

        final ConstantModel<Object, Double, Option<Double>> sub =
                new ConstantModel<>(idI, cI, aO.auditor(mI).get());

        // Need to cast b/c of the higher-kinded types in HierarchicalConstantModel.apply's
        // 4th input parameter. Scala emits an Object when as the expected type because the
        // JVM doesn't have a notion of higher-kinded types.  So it expects
        // Submodel<Object, Double, Object>.  Therefore, we need to take sub, which has the
        // proper typing, and dumb it down.  Unfortunately, we run into issues if we try to
        // cast directly; therefore, we need to cast to a capture then to Object, hence the
        // two casts.  Yay "Seemless Java Interop"!
        // http://www.scala-lang.org/what-is-scala.html#seamless-java-interop
        final Submodel<Object, Double, Object> casted =
                (Submodel<Object, Double, Object>)
                (Submodel<Object, Double, ?>) sub;

        final HierarchicalConstantModel<Object, Double, Float, Option<Float>> model =
                HierarchicalConstantModel$.MODULE$.apply(idO, cO, aO, casted);

        final Option<Float> y = model.apply(cI);
        assertEquals(Option.apply(cO), y);
    }

    @Test
    public void test1() throws JavaModelFactoryException {
        final Float constant = 1f;
        final Model<Object, Option<Float>> m = getModel(constant);
        final Option<Float> value = m.apply(null);
        assertEquals(Option.apply(constant), value);
    }

    @Test
    public void testTypeInference() throws JavaModelFactoryException {
        final ModelId id = new ModelId(1, "test");
        final Manifest<Double> m = manifest("java.lang.Double");
        final Semantics<String> s = new Semantics<String>();
        final OptionAuditor<ModelId, Double> a = new OptionAuditor<ModelId, Double>(m);
        final Double v = 1.234d;

        // The point here is that no types of any kind are explicitly provided here.
        assertEquals(Option.apply(v), JavaModelFactories.create(a).createConstantModel(s, id, v).apply(null));
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
        final OptionAuditor<ModelId, Float> na = new OptionAuditor<ModelId, Float>(refInfo);
        final JavaModelFactory<Float, Option<Float>, OptionAuditor<ModelId, Float>> factory = JavaModelFactories.create(na);
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
