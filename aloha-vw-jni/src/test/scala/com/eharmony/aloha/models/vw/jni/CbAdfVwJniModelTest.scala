package com.eharmony.aloha.models.vw.jni

import java.io.File

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.test.proto.ContextActionsProtoBuffs.ContextActionsProto
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.test.proto.ContextActionsProtoBuffs.ContextActionsProto.{ActionProto, ActionsProto, ContextProto}
import spray.json.DefaultJsonProtocol.{IntJsonFormat, _}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by sahil-goyal on 11/17/16.
  */
object CbAdfVwJniModelTest {

  /**
    * The path to an existent VW contextual bandit model.  Given the semantics, and model features, the
    * model should always return the class 2, which is the index one into the classLabels array if provided.
    */
  private[jni] lazy val cbVwModelPath = {
    val tf = File.createTempFile("vwcbadf_", ".model")
    tf.deleteOnExit()
    val p = tf.getCanonicalPath

    val vw: VWActionScoresLearner = VWLearners.create(s"--cb_adf --quiet -f $p")
    val input = Vector(
      Array("| a:1 b:0.5", "0:0.1:0.75 | a:0.5 b:1 c:2"),
      Array("shared | s_1 s_2", "0:1.0:0.5 | a:1 b:1 c:1", "| a:0.5 b:2 c:1")
    )
    for {
      i <- 1 to 100
      example <- input
    } vw.learn(example)
    vw.close()
    p
  }

  /**
    * Semantics where no features are pulled from the context.
    */
  private[jni] def factory(outputType: String) = {
    val semantics = CompiledSemantics[ContextActionsProto](TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
      CompiledSemanticsProtoPlugin[ContextActionsProto],
      Seq("com.eharmony.aloha.feature.BasicFunctions._"))
    outputType match {
      case "Double" => ModelFactory.defaultFactory.toTypedFactory[ContextActionsProto, Double](semantics)
      case "String" => ModelFactory.defaultFactory.toTypedFactory[ContextActionsProto, String](semantics)
      case "Byte" => ModelFactory.defaultFactory.toTypedFactory[ContextActionsProto, Byte](semantics)
      case "Short" => ModelFactory.defaultFactory.toTypedFactory[ContextActionsProto, Short](semantics)
      case "Int" => ModelFactory.defaultFactory.toTypedFactory[ContextActionsProto, Int](semantics)
      case "Long" => ModelFactory.defaultFactory.toTypedFactory[ContextActionsProto, Long](semantics)
      case "Float" => ModelFactory.defaultFactory.toTypedFactory[ContextActionsProto, Float](semantics)
      case _ => throw new UnsupportedOperationException(s"Method does not support factory of output type $outputType")
    }
  }

}

@RunWith(classOf[BlockJUnit4ClassRunner])
class CbAdfVwJniModelTest {
  import CbAdfVwJniModelTest._

  private[this] val input1 = ContextActionsProto.newBuilder
    .setActions(
      ActionsProto.newBuilder
        .addActions(ActionProto.newBuilder
          .setActionIdDouble(45.0)
          .setActionIdString("45")
          .setActionIdInt(45)
          .setActionIdLong(45L)
          .setActionIdFloat(45.0f)
          .setF1(1).setF2(0.5f))
        .addActions(ActionProto.newBuilder
          .setActionIdDouble(46.0)
          .setActionIdString("46")
          .setActionIdInt(46)
          .setActionIdLong(46L)
          .setActionIdFloat(46.0f)
          .setF1(0.5f).setF2(1).setF3(2))
    ).build
  private[this] val input2 = ContextActionsProto.newBuilder
    .setContext(ContextProto.newBuilder.setF1(1).setF2(1))
    .setActions(
      ActionsProto.newBuilder
        .addActions(ActionProto.newBuilder
          .setActionIdDouble(47.0)
          .setActionIdString("47")
          .setActionIdInt(47)
          .setActionIdLong(47L)
          .setActionIdFloat(47.0f)
          .setF1(1).setF2(1).setF3(1))
        .addActions(ActionProto.newBuilder
          .setActionIdDouble(48.0)
          .setActionIdString("48")
          .setActionIdInt(48)
          .setActionIdLong(48L)
          .setActionIdFloat(48.0f)
          .setF1(0.5f).setF2(2).setF3(1))
    ).build

  private[this] val baseJson =
    s"""
       |{
       |  "modelType": "VwJNI",
       |  "modelId": { "id": 0, "name": "" },
     """.stripMargin

  private[this] def addFeatures(json: String): String = {
    s"""
       |$json
       |  "features": {
       |    "s_1": "$${context.f1}",
       |    "s_2": "$${context.f2}"
       |  },
     """.stripMargin
  }

  private[this] def addLabelDomain(json: String): String = {
    s"""
       |$json
       |  "labelDomain": "$${actions.actions}",
     """.stripMargin
  }

  private[this] def addLabelType(json: String): String = {
    s"""
       |$json
       |  "labelType": "com.eharmony.aloha.test.proto.ContextActionsProtoBuffs.ContextActionsProto.ActionProto",
     """.stripMargin
  }

  private[this] def addLabelDependentFeatures(json: String): String = {
    s"""
       |$json
       |  "labelDependentFeatures": {
       |    "a": "$${f1}",
       |    "b": "$${f2}",
       |    "c": "$${f3}"
       |  },
     """.stripMargin
  }

  private[this] def addScoreExtractor(json: String, scoreExtractor: String): String = {
    s"""
       |$json
       |  "scoreExtractor": "$scoreExtractor",
     """.stripMargin
  }

  private[this] def addVwParams(json: String): String = {
    s"""
       |$json
       |  "vw": {
       |    "modelUrl": "$cbVwModelPath",
       |    "params": [
       |      "-t",
       |      "--quiet"
       |    ]
       |  }
       |}
     """.stripMargin
  }

  @Test def testCbAdfWithoutFeatures(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelType(addLabelDomain(baseJson))), "${action_id_int}"))
    val model = factory("Int").fromString(json).get
    val pred1 = model(input1).get
    val pred2 = model(input2).get
    assertEquals((46, 48), (pred1, pred2))
  }

  @Test(expected = classOf[IllegalArgumentException]) def testCbAdfWithoutLabelDomain(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelType(addFeatures(baseJson))), "${action_id_int}"))
    val model = factory("Int").fromString(json).get
  }

  @Test(expected = classOf[IllegalArgumentException]) def testCbAdfWithoutLabelType(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelDomain(addFeatures(baseJson))), "${action_id_int}"))
    val model = factory("Int").fromString(json).get
  }

  @Test(expected = classOf[IllegalArgumentException]) def testCbAdfWithoutLabelDependentFeatures(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelType(addLabelDomain(addFeatures(baseJson))), "${action_id_int}"))
    val model = factory("Int").fromString(json).get
  }

  @Test(expected = classOf[IllegalArgumentException]) def testCbAdfWithoutScoreExtractor(): Unit = {
    val json = addVwParams(addLabelDependentFeatures(addLabelType(addLabelDomain(addFeatures(baseJson)))))
    val model = factory("Int").fromString(json).get
  }

  @Test def testCbAdfWithDoubleLabels(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelType(addLabelDomain(addFeatures(baseJson)))), "${action_id_double}"))
    val model = factory("Double").fromString(json).get
    val pred1 = model(input1).get
    val pred2 = model(input2).get
    assertEquals((46.0, 48.0), (pred1, pred2))
  }

  @Test def testCbAdfWithStringLabels(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelType(addLabelDomain(addFeatures(baseJson)))), "${action_id_string}"))
    val model = factory("String").fromString(json).get
    val pred1 = model(input1).get
    val pred2 = model(input2).get
    assertEquals(("46", "48"), (pred1, pred2))
  }

  @Test def testCbAdfWithIntLabels(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelType(addLabelDomain(addFeatures(baseJson)))), "${action_id_int}"))
    val model = factory("Int").fromString(json).get
    val pred1 = model(input1).get
    val pred2 = model(input2).get
    assertEquals((46, 48), (pred1, pred2))
  }

  @Test def testCbAdfWithLongLabels(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelType(addLabelDomain(addFeatures(baseJson)))), "${action_id_long}"))
    val model = factory("Long").fromString(json).get
    val pred1 = model(input1).get
    val pred2 = model(input2).get
    assertEquals((46L, 48L), (pred1, pred2))
  }

  @Test def testCbAdfWithFloatLabels(): Unit = {
    val json = addVwParams(addScoreExtractor(addLabelDependentFeatures(addLabelType(addLabelDomain(addFeatures(baseJson)))), "${action_id_float}"))
    val model = factory("Float").fromString(json).get
    val pred1 = model(input1).get
    val pred2 = model(input2).get
    assertEquals((46.0f, 48.0f), (pred1, pred2))
  }

}