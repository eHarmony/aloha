package com.eharmony.aloha.score.audit;

import com.eharmony.aloha.id.ModelId;
import com.eharmony.aloha.id.ModelIdentity;
import com.eharmony.aloha.score.audit.EitherAuditor.Result;
import com.eharmony.aloha.score.audit.EitherAuditor.Success;
import com.eharmony.aloha.score.audit.JavaModelFactory.JavaModelFactoryException;
import deaktator.reflect.runtime.manifest.ManifestParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;
import scala.reflect.Manifest;

import static org.junit.Assert.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class JavaAuditTest {
    @Test
    public void testHierarchical() {
        final Double cI = 1d;
        final ModelId idI = new ModelId(1, "one");
        final Manifest<Float> mO = manifest("java.lang.Float");
        final OptionAuditor<ModelIdentity, Float> aO = new OptionAuditor<ModelIdentity, Float>(mO);
        final Manifest<Double> mI = manifest("java.lang.Double");
        final ConstantModel<Object, Double, Option<Double>> sub =
                new ConstantModel<>(idI, cI, aO.auditor(mI).get());

        final Float cO = 2f;
        final ModelId idO = new ModelId(2, "two");

        // Need to cast b/c of the type constructor application in
        // `HierarchicalConstantModel.apply`'s 4th input parameter. Scala emits an Object
        // when as the expected type because the JVM doesn't have a notion of type constructors.
        // So it expects Submodel<Object, Double, Object>.  Therefore, we need to take sub,
        // which has the proper typing, and dumb it down.  Unfortunately, we run into issues if
        // we try to cast directly; therefore, we need to cast to a capture then to Object, hence
        // the two casts.  Unfortunately, all of the casting leads to compiler warnings that we
        // can suppress.
        // "Seemless Java Interop"!  http://www.scala-lang.org/what-is-scala.html#seamless-java-interop

        @SuppressWarnings("unchecked")
        final AuditedModel<Object, Double, Object> casted =
                (AuditedModel<Object, Double, Object>)
                (AuditedModel<Object, Double, ?>) sub;

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
        final Manifest<Double> mD = manifest("java.lang.Double");
        final Manifest<String> mS = manifest("java.lang.String");
        final OptionAuditor<ModelIdentity, Double> aud = new OptionAuditor<ModelIdentity, Double>(mD);
        final Semantics<String> sem = new Semantics<String>(mS);
        final Double constant = 1.234d;

        final Option<Double> y =  // *IMPORTANT*: Notice no types are explicitly provided below:
            new JavaModelFactory(new StdModelFactory())       // Create Factory.
                .createConstantModel(sem, aud, id, constant)  // Create model.
                .apply(null);                             // Call model.

        assertEquals(Option.apply(constant), y);
    }

    @Test
    public void testEitherAuditor() throws JavaModelFactoryException {
        final ModelId id = new ModelId(1, "test");
        final Manifest<Double> m = manifest("java.lang.Double");
        final Manifest<String> mS = manifest("java.lang.String");
        final EitherAuditor<ModelIdentity, Double> aud = new EitherAuditor<ModelIdentity, Double>(m);
        final Semantics<String> sem = new Semantics<String>(mS);
        final Double constant = 1.234d;

        final Result<ModelIdentity, Double> y =  // *IMPORTANT*: Notice no types are explicitly provided below:
            new JavaModelFactory(new StdModelFactory())       // Create Factory.
                .createConstantModel(sem, aud, id, constant)  // Create model.
                .apply(null);                             // Call model.

        if (y instanceof Success) {
            final Success<ModelIdentity, Double> s = (Success<ModelIdentity, Double>) y;
            assertEquals(id.getId(), s.key().getId());
            assertEquals(id.getName(), s.key().getName());
            assertTrue(s.childValues().isEmpty());
            assertTrue(s.missingVarNames().isEmpty());
            assertEquals(constant, s.valueToAudit(), 0d);
            assertTrue(s.prob().isEmpty());
        }
        else fail("Expected a Success, found: \n" + y);
    }


    /**
     * Steps to creating a model in Java:
     * <ol>
     *     <li>Create an Aloha RefInfo <em>(scala.reflect.Manifest)</em>.</li>
     *     <li>Create a MorphableAuditor.</li>
     *     <li>Create JavaModelFactory around a ModelFactory.</li>
     *     <li>Create or pass in Aloha Semantics</li>
     *     <li>Use JavaModelFactory with Semantics, a MorphableAuditor and a source to create the model.</li>
     * </ol>
     * @return a Model
     * @throws JavaModelFactoryException when a model can't be created
     */
    private static Model<Object, Option<Float>> getModel(final Float constant) throws JavaModelFactoryException {
        final Manifest<Float> refInfo = manifest("java.lang.Float");
        final Manifest<Object> refInfoObj = manifest("java.lang.Object");
        final OptionAuditor<ModelIdentity, Float> aud = new OptionAuditor<ModelIdentity, Float>(refInfo);
        final JavaModelFactory factory = new JavaModelFactory(new StdModelFactory());
        final Semantics<Object> semantics = new Semantics<Object>(refInfoObj);
        final ModelId modelId = new ModelId(1, "test");
        return factory.createConstantModel(semantics, aud, modelId, constant);
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
