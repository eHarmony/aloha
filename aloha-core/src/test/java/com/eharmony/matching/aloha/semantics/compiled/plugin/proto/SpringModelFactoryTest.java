package com.eharmony.matching.aloha.semantics.compiled.plugin.proto;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import scala.util.Try;

import com.eharmony.matching.aloha.io.AlohaReadable;
import com.eharmony.matching.aloha.models.Model;
import com.eharmony.matching.aloha.score.Scores.Score;
import com.eharmony.matching.aloha.score.conversions.StrictConversions;
import com.eharmony.matching.aloha.test.proto.TestProtoBuffs.TestProto;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/model.cfg.xml" })
public class SpringModelFactoryTest {

	static final double TOLERANCE = 1.0e-7;

	static final List<TestProto> PROTOS = getTestExamples();

	/**
	 * Expected value of running CompiledSemanticsProtoPluginTest.getProto(1)
	 * through the model.
	 *
	 * 0.12421 = 0.100000 + 0.020000 + 0.004000 + 0.000200 + 0.000010
	 *
	 * Features: intercept -> List((intercept,1.0)) num_c3s ->
	 * List((num_c3s=0,1.0)) required_int_value ->
	 * List((required_int_value=1,1.0)) optional_defaulted ->
	 * List((optional_defaulted=1,1.0)) distinct_chars ->
	 * List((distinct_chars=3,1.0))
	 */
	static final double EXPECTED_1 = 0.12421;

	@Resource
	private FileObject modelJson1 = null;

	/**
	 * IMPORTANT: Needs to be referred to by the interface AlohaReadable rather
	 * than the actual class name. This is because java doesn't play nice with
	 * higher kinds. See http://en.wikipedia.org/wiki/Kind_(type_theory) for
	 * more information on kinds.
	 */
	@Resource
	private AlohaReadable<Try<Model<TestProto, Double>>> modelFactory = null;

	@Resource
	private List<Model<TestProto, Double>> models = null;

	@Test
	public void testUsingModelFactoryPulledFromSpringCtxAndCallingFileObject() {
		final Model<TestProto, Double> model = modelFactory
				.fromVfs2(modelJson1).get();
		final TestProto p = PROTOS.get(1);
		final Score s = model.score(p);
		final Double d = StrictConversions.asJavaDouble(s);
		assertEquals(EXPECTED_1, d, TOLERANCE);
	}

	@Test
	public void testUsingModelPulledFromSpringCtx() {
		final TestProto p = PROTOS.get(1);
		System.out.println("testing proto: " + p);
		final Score s = models.get(0).score(p);
		final Double d = StrictConversions.asJavaDouble(s);
		assertEquals(EXPECTED_1, d, TOLERANCE);

		final Double d1 = StrictConversions
				.asJavaDouble(models.get(1).score(p));
		assertEquals(EXPECTED_1, d1, TOLERANCE);
	}

	/**
	 * Load a bunch of TestProto instances. This is stupid because they are the
	 * same as the ones in CompiledSemanticsProtoPluginTest.protos but b/c of
	 * compile order issues in maven, those are not present at the time this
	 * test is run. So, they were serialized and stuck in the src/test/resources
	 * directory.
	 * 
	 * @return TestProto instances.
	 */
	private static List<TestProto> getTestExamples() {
		final ArrayList<TestProto> protos = new ArrayList<TestProto>(8);
		try {
			// Sloppy but whatever.
			for (int i = 0; i <= 7; ++i) {
				final InputStream is = VFS
						.getManager()
						.resolveFile(
								"res:spring_test/pb_instances/level_" + i
										+ ".pb").getContent().getInputStream();
				final TestProto p = TestProto.parseFrom(is);
				is.close();
				protos.add(p);
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e.getCause());
		}
		return Collections.unmodifiableList(protos);
	}
}
