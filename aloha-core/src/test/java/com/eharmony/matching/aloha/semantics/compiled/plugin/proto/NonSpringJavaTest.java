package com.eharmony.matching.aloha.semantics.compiled.plugin.proto;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import scala.util.Try;

import com.eharmony.matching.aloha.factory.ModelFactory;
import com.eharmony.matching.aloha.factory.ModelParser;
import com.eharmony.matching.aloha.factory.TypedModelFactory;
import com.eharmony.matching.aloha.interop.DoubleFactoryInfo;
import com.eharmony.matching.aloha.interop.IntegerFactoryInfo;
import com.eharmony.matching.aloha.models.Model;
import com.eharmony.matching.aloha.models.reg.RegressionModel;
import com.eharmony.matching.aloha.score.Scores.Score;
import com.eharmony.matching.aloha.score.conversions.StrictConversions;
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics;
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler;
import com.eharmony.matching.aloha.test.proto.TestProtoBuffs.TestProto;

@RunWith(BlockJUnit4ClassRunner.class)
public class NonSpringJavaTest {

	/**
	 * Location of a JVM .class file cache directory.
	 */
	private static final String CLASS_CACHE_DIR;

	/**
	 * A list of imports for use in the semantics.
	 */
	private static final String[] SEMANTICS_IMPORTS;

	/**
	 * Location of the model.
	 */
	private static final String VFS2_MODEL_LOCATION_URL_STRING;

	/**
	 * Apache VFS 2 File Manager.
	 */
	private static final FileSystemManager vfs2FileManager;

	static {
		try {
			vfs2FileManager = VFS.getManager();
			final Properties props = getTestProps();
			CLASS_CACHE_DIR = props.getProperty("testGeneratedClasses");
			SEMANTICS_IMPORTS = props.getProperty("semantics_imports").split(
					",");
			VFS2_MODEL_LOCATION_URL_STRING = props
					.getProperty("model_location_1");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * A test showing how to instantiate the factory and model directly from
	 * Java (instead of Spring). Note that the model code is not really
	 * idiomatic because it copies the control flow
	 * 
	 * @throws FileSystemException
	 */
	@Test
	public void test1() throws FileSystemException // b/c VFS.getManager();
	{

		// Construct the factory. Can use one factory for many models so long as
		// the type parameters are the same.
		final TypedModelFactory<TestProto, Double> modelFactory = getModelFactory();

		// Construct the model. Can reuse the models. All models should be
		// thread safe and lock free.
		final Model<TestProto, Double> model = getModel(modelFactory,
				vfs2FileManager.resolveFile(VFS2_MODEL_LOCATION_URL_STRING));

		// Test that the model works.
		testModel(model);
	}

	/**
	 * This is the code actually used to test the correctness of the model. Same
	 * as in the SpringModelFactoryTest.
	 * 
	 * @param model
	 *            a model to test
	 */
	private static void testModel(Model<TestProto, Double> model) {
		final TestProto p = SpringModelFactoryTest.PROTOS.get(1);
		final Score s = model.score(p);
		final Double d = StrictConversions.asJavaDouble(s);
		assertEquals(SpringModelFactoryTest.EXPECTED_1, d,
				SpringModelFactoryTest.TOLERANCE);
	}

	/**
	 * Construct a model given a factory. This code isn't exactly production
	 * code but should be good for illustrative purposes.
	 * 
	 * @param modelFactory
	 *            a factory
	 * @param fo2Model
	 *            a file from which to construct a model
	 * @return a model specified by the file.
	 */
	private static Model<TestProto, Double> getModel(
			TypedModelFactory<TestProto, Double> modelFactory,
			org.apache.commons.vfs2.FileObject fo2Model) {

		// THIS CAST IS NECESSARY (even though it might not seem like it):
		// (Scala compiler bug)
		// Get an attempt to get the model.
		final Try<Model<TestProto, Double>> modelTry = (Try<Model<TestProto, Double>>) modelFactory
				.fromVfs2(fo2Model);

		// modelTry.isSuccess() tells if there is something inside:
		// - true is good.
		// - false means calling .get() function will throw.
		if (modelTry.isSuccess()) {
			return modelTry.get();
		} else {
			// This will definitely throw (b/c isSuccess() == false) but JVM
			// doesn't know because it is unchecked.
			// For more information, see API:
			// http://www.scala-lang.org/api/2.10.1/index.html#scala.util.Try
			try {
				return modelTry.get();
			} catch (Exception e) {
				throw new RuntimeException(
						"Rethrowing exception for kicks (SHOULDN'T NECESSARILY DO THIS IN PROD)",
						e);
			}
		}
	}

	/**
	 * Create a model factory from Java.
	 *
	 * Notice there are some casting issues because the code was written to be
	 * useful in Scala and the code employs some programming concepts (e.g.
	 * higher kinds) not easily reducible to a java programming concept.
	 * 
	 * @return a factory used to construct models.
	 */
	private static TypedModelFactory<TestProto, Double> getModelFactory() {

		// ================================================================================================
		// Construct the semantics
		// ================================================================================================

		// ------------------------------------------------------------------------------------------------
		// Construct the proto plugin instance that will handle protocol buffer
		// based input
		// ------------------------------------------------------------------------------------------------
		final CompiledSemanticsProtoPlugin<TestProto> userPairingProtoPlugin = new CompiledSemanticsProtoPlugin<TestProto>(
				TestProto.class);

		// ------------------------------------------------------------------------------------------------
		// Construct the compiler instance responsible for compiling the
		// synthesized model features.
		//
		// NOTE: In production, it is important to specify a class cache
		// directory if the model or
		// any of it's features are intended to be used more than one time. This
		// won't help if
		// the features have never been compiled; however, if they have, a class
		// cache directory
		// will make the compilation process many times faster because the JVM
		// class files don't
		// need to be recompiled.
		// ------------------------------------------------------------------------------------------------
		final TwitterEvalCompiler compiler = new TwitterEvalCompiler(new File(
				CLASS_CACHE_DIR));

		// ------------------------------------------------------------------------------------------------
		// Construct the semantics. This is the object that gives meaning to the
		// features. I.e., it
		// transforms them from specifications to working functions.
		// ------------------------------------------------------------------------------------------------
		final CompiledSemantics<TestProto> semantics = new CompiledSemantics(
				compiler, userPairingProtoPlugin, SEMANTICS_IMPORTS,
				scala.concurrent.ExecutionContext$.MODULE$.global());

		// ================================================================================================
		// Add each model parser that we want the factory to know about.
		// Eventually, there will be a
		// standard list of parsers in aloha-core so any factory will easily be
		// able to include all
		// parsers.
		// ================================================================================================
		final ArrayList<ModelParser> parsers = new ArrayList<ModelParser>();
		parsers.add(RegressionModel.parser());
		RegressionModel.parser();

		// ================================================================================================
		// Create an untyped factory. This isn't much use in Java because it
		// makes use of higher kinds.
		// We want to convert it to a typed factory by providing semantics, and
		// reflection information
		// about the data types that the models consume and produce.
		// ================================================================================================
		final ModelFactory untypedModelFactory = new ModelFactory(parsers);

		// ================================================================================================
		// Create the typed factory. This factory produces models that take
		// TestProto instances as
		// input and produces ieee-754 64-bit floats as output.
		//
		// In general, calling the score(..) function that returns a proto is
		// going to be the most
		// useful in a production setting because it contains type-safe return
		// values along with an
		// audit trail and error messages and it logs missing data.
		// ================================================================================================
		final TypedModelFactory<TestProto, Double> testProtoToDoubleModelFactory = untypedModelFactory
				.toTypedFactory(semantics, new DoubleFactoryInfo<TestProto>(
						TestProto.class));

		// ------------------------------------------------------------------------------------------------
		// This just shows that we can easily construct another factory for
		// different output types
		// while reusing the semantics, etc.
		// ------------------------------------------------------------------------------------------------
		final TypedModelFactory<TestProto, Integer> testProtoToIntegerModelFactory = untypedModelFactory
				.toTypedFactory(semantics, new IntegerFactoryInfo<TestProto>(
						TestProto.class));

		return testProtoToDoubleModelFactory;
	}

	private static Properties getTestProps() {
		final Properties props = new Properties();

		try {
			props.load(vfs2FileManager
					.resolveFile("res:mvn_gen_test.properties").getContent()
					.getInputStream());
			props.load(vfs2FileManager
					.resolveFile("res:spring_test/spring_test.properties")
					.getContent().getInputStream());
			return props;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
