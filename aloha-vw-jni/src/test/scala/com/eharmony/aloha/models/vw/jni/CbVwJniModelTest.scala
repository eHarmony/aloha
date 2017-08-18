package com.eharmony.aloha.models.vw.jni

import java.io.File

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLines}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsonFormat}
import vowpalWabbit.learner.{VWLearners, VWMulticlassLearner}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

object CbVwJniModelTest {

  /**
   * The path to an existent VW contextual bandit model.  Given the semantics, and model features, the
   * model should always return the class 2, which is the index one into the classLabels array if provided.
   */
  private[jni] lazy val cbVwModelPath = {
    val tf = File.createTempFile("vwcb_", ".model")
    tf.deleteOnExit()
    val p = tf.getCanonicalPath

    val vw = VWLearners.create[VWMulticlassLearner](s"--cb 2 --quiet -f $p")
    val input = Vector("1:2:0.5 | a c",
                       "2:1:0.5 | b c")
    for {
      i <- 1 to 100
      example <- input
    } vw.learn(example)
    vw.close()
    p
  }

  private[jni] lazy val semantics =
    CompiledSemantics(TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
      CompiledSemanticsCsvPlugin(),
      Seq("com.eharmony.aloha.feature.BasicFunctions._"))

  private[jni] val emptyLine = CsvLines(Map.empty).apply("")
}

@RunWith(classOf[BlockJUnit4ClassRunner])
class CbVwJniModelTest {
  import CbVwJniModelTest._

  @Test def testWithLongLabels(): Unit = {
    val factory = ModelFactory.defaultFactory(semantics, OptionAuditor[Long]())
    val model = factory.fromString(json(Seq(0L, 1L))).get
    val pred = model(emptyLine).get
    assertEquals("long", pred.getClass.getSimpleName)
    assertEquals(1L, pred)
  }

  @Test def testWithDoubleLabels(): Unit = {
    val factory = ModelFactory.defaultFactory(semantics, OptionAuditor[Double]())
    val model = factory.fromString(json(Seq(0d, 1d))).get
    val pred = model(emptyLine).get
    assertEquals("double", pred.getClass.getSimpleName)
    assertEquals(1d, pred, 0)
  }

  @Test def testWithBooleanLabels(): Unit = {
    val factory = ModelFactory.defaultFactory(semantics, OptionAuditor[Boolean]())
    val jsonStr = json(Seq(false, true))
    val modelTry = factory.fromString(jsonStr)
    val model = modelTry.get
    val pred = model(emptyLine).get
    assertEquals(true, pred)
  }

  @Test def testWithStringLabels(): Unit = {
    val factory = ModelFactory.defaultFactory(semantics, OptionAuditor[String]())
    val model = factory.fromString(json(Seq("Career", "Family"))).get
    val pred = model(emptyLine).get
    assertEquals("Family", pred)
  }

  @Test def testWithoutLabels(): Unit = {
    val factory = ModelFactory.defaultFactory(semantics, OptionAuditor[Int]())
    val model = factory.fromString(json).get
    val pred = model(emptyLine).get
    assertEquals(2, pred)
  }

  /**
   * Test that repeated labels in classLabels array causes a ''scala.util.Failure'' at model creation time.
   */
  @Test def testRepeatedLabels(): Unit = {
    val factory = ModelFactory.defaultFactory(semantics, OptionAuditor[Boolean]())

    factory.fromString(json(Seq(false, false))) match {
      case Failure(e: DeserializationException) if e.getMessage == "Couldn't produce SimpleTypeSeq for array: [false,false]" =>
      case d => fail(s"Should throw exception. Found: $d")
    }
  }

  /**
   * Create JSON model with no classLabels array.  Is input invariant and points to the binary VW model
   * in the file ''CbVwJniModelTest.cbVwModelPath''.
   * @return
   */
  private[this] def json: String =
    s"""
       |{
       |  "modelType": "VwJNI",
       |  "modelId": { "id": 0, "name": "" },
       |  "features": {
       |    "b": "Seq((\\"\\", 1.0))",
       |    "c": "Seq((\\"\\", 1.0))"
       |  },
       |  "vw": {
       |    "modelUrl": "$cbVwModelPath",
       |    "params": [
       |      "-t",
       |      "--quiet"
       |    ]
       |  }
       |}
     """.stripMargin.trim

  /**
   * Create JSON model with classLabels array.  Is input invariant and points to the binary VW model
   * in the file ''CbVwJniModelTest.cbVwModelPath''.
   * @param classLabels class Labels for prediction classes
   * @tparam A type of labels.
   * @return
   */
  private[this] def json[A: JsonFormat](classLabels: Seq[A]): String = {
    // Can't put --cb flag into vw params.
    val js = s"""
                |{
                |  "modelType": "VwJNI",
                |  "modelId": { "id": 0, "name": "" },
                |  "features": {
                |    "b": "Seq((\\"\\", 1.0))",
                |    "c": "Seq((\\"\\", 1.0))"
                |  },
                |  "vw": {
                |    "modelUrl": "$cbVwModelPath",
                |    "params": [
                |      "-t",
                |      "--quiet"
                |    ]
                |  }
              """.stripMargin.trim

    js + ",\n  \"classLabels\": " + implicitly[JsonFormat[Seq[A]]].write(classLabels).compactPrint + "\n}"
  }
}
