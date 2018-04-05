package com.eharmony.aloha.models.h2o

import com.eharmony.aloha.{FileLocations, ModelSerializationTestHelper}
import com.eharmony.aloha.audit.impl.tree.{NubRootedTree, RootedTreeAuditor, RootedTreeImpl, Tree}
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.vfs.{Vfs, VfsType}
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.models.h2o.H2oModel.Features
import com.eharmony.aloha.models.h2o.json.{DoubleH2oSpec, H2oSpec}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc}
import com.eharmony.aloha.test.proto.TestProtoBuffs.Abalone
import com.eharmony.aloha.test.proto.TestProtoBuffs.Abalone.Gender.{FEMALE, INFANT, MALE}
import com.eharmony.aloha.util.Logging
import hex.genmodel.easy.RowData
import org.apache.commons.vfs2.VFS
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol._
import spray.json.pimpString

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.{immutable => sci}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import scala.util.Try

/**
  * Test H2oModel
  * Created by deak on 10/23/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class H2oModelTest extends Logging with ModelSerializationTestHelper {
  import H2oModelTest._

  @Test def testSerialization(): Unit = {
    val h2oModelClass = classOf[H2oModel[_, _, _, _]]
    val unserialized = AbaloneModel match {
      case x: H2oModel[Any, Float, Abalone, NubRootedTree[Float]] => x
      case _ =>
        fail(s"AbaloneModel should be an ${h2oModelClass.getCanonicalName}")
        throw new IllegalStateException("NEVER GET HERE!")
    }

    assertTrue(h2oModelClass isAssignableFrom unserialized.getClass)
    val serialized = serializeDeserializeRoundTrip(unserialized)
    assertTrue(h2oModelClass isAssignableFrom serialized.getClass)
  }

  @Test def testParseSpecNoTypeIsDouble(): Unit = {
    val json = """{ "myFeatureName": { "spec": "${length}", "defVal": -654321 } }"""
    val mapSeq = json.parseJson.convertTo[sci.ListMap[String, H2oSpec]].toSeq

    assertEquals(1, mapSeq.size)
    val name = mapSeq.head._1
    val spec = mapSeq.head._2
    val expectedName = "myFeatureName"
    val expectedSpec = DoubleH2oSpec(expectedName, "${length}", Some(-654321))

    assertEquals(expectedName, name)
    assertEquals(expectedSpec, spec)
  }

  @Test def testConstructFeatures(): Unit = {
    val json =
      """
        |{
        |  "modelType": "H2o",
        |  "modelId": { "id": 0, "name": "proto model" },
        |  "features": {
        |    "sex_none_def": { "spec": "None map (_ => ${sex}.name)",    "defVal": "MISSING", "type": "string" },
        |    "sex_none_no":  { "spec": "None map (_ => ${sex}.name)",                         "type": "string" },
        |    "sex_some_def": { "spec": "${sex}.name",                    "defVal": "MISSING", "type": "string" },
        |    "sex_some_no":  { "spec": "${sex}.name",                                         "type": "string" },
        |    "len_none_def": { "spec": "None map (_ => ${length})",      "defVal": -564321,   "type": "double" },
        |    "len_none_no":  "None map (_ => ${length})",
        |    "len_some_def": { "spec": "${length}",                      "defVal": -564321,   "type": "double" },
        |    "len_some_no":  { "spec": "${length}",                                           "type": "double" }
        |  },
        |  "modelUrl": "res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
        |}
      """.stripMargin.trim

    val model = ProtoFactory[Float].fromString(json).get.asInstanceOf[H2oModel[Tree[_], Float, Abalone, NubRootedTree[Float]]]

    val x = AbaloneData.head
    val y: Features[RowData] = model.constructFeatures(x)
    val rowData = mapAsScalaMap(y.features).toMap

    assertEquals(6, rowData.size)

    assertEquals("MISSING", rowData("sex_none_def").asInstanceOf[String])
    assertEquals("MALE", rowData("sex_some_no").asInstanceOf[String])
    assertEquals("MALE", rowData("sex_some_def").asInstanceOf[String])

    assertEquals(-564321.0, rowData("len_none_def").asInstanceOf[java.lang.Double].doubleValue(), 0)
    assertEquals(0.45500001311302185, rowData("len_some_def").asInstanceOf[java.lang.Double].doubleValue(), 0)
    assertEquals(0.45500001311302185, rowData("len_some_no").asInstanceOf[java.lang.Double].doubleValue(), 0)

    assertFalse(rowData.contains("sex_none_no"))
    assertFalse(rowData.contains("len_none_no"))
  }

  @Test def testVectorOnEmptyData(): Unit = {
    val model = AbaloneVecModel

    val empty = Abalone.getDefaultInstance

    val expMissing = Set(
      "diameter",
      "height",
      "length",
      "sex",
      "weight.shell",
      "weight.shucked",
      "weight.viscera",
      "weight.whole"
    )

    val expEmptyOut =
      RootedTreeImpl(
        modelId = ModelId(0, "proto model"),
        errorMsgs = Vector(
          "H2o model may have encountered a missing categorical variable.  Likely features: Sex",
          "See: vec_glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.score0(vec_glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java:59)"
        ),
        missingVarNames = expMissing,
        value = None,
        subvalues = Vector.empty,
        prob = None
      )

    val emptyOut = model(empty)
    assertEquals(expEmptyOut, emptyOut)
  }

  @Test def testVector(): Unit = {
    // This is like the testProto test, except that the features are combined into a double vector.
    // The underlying h2o model used in testProto was copied and changed so that the feature names
    // line up with the feature names emitted by the Aloha wrapper.

    val model = AbaloneVecModel

    val data = ExpectedAbaloneModelResults.zip(AbaloneData).zipWithIndex
    data foreach { case ((exp, abalone), i) =>

      // The prediction loop:  predict, given a native input type of the caller's choosing.
      val act: Option[Float] = model(abalone).value
      assertEquals(s"in test $i", exp, act.get, Epsilon)
    }
  }

  @Test def testProto(): Unit = {
    val model = AbaloneModel

    // Test predictions are correct.
    // To test in parallel, do something like  // val data = Vector.fill(1000)(AbaloneData.toVector).flatten.par
    val data = ExpectedAbaloneModelResults.zip(AbaloneData).zipWithIndex
    data foreach { case ((exp, abalone), i) =>

      // The prediction loop:  predict, given a native input type of the caller's choosing.
      val act = model(abalone).value
      assertEquals(s"in test $i", exp, act.get, Epsilon)
    }
  }

  @Test def testProtoEg(): Unit = {
    val model = AbaloneModel

    // For expository purposes:
    val input: Abalone            = AbaloneData.head
    val expected: Double          = ExpectedAbaloneModelResults.head
    val out: NubRootedTree[Float] = model(input)
    val actual: Option[Float]     = out.value
    assertEquals(expected, actual.get, Epsilon)
  }

  @Test def testEmptyGLM(): Unit = {
    val res = AbaloneModel(Abalone.getDefaultInstance)

    val expectedErrors = Seq(
      "H2o model may have encountered a missing categorical variable.  Likely features: Sex",
      "See: glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.score0(glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java:59)"
    )

    assertEquals(None, res.value)
    assertEquals(expectedErrors, res.errorMsgs)
    assertEquals(AbaloneModelFeatures, res.missingVarNames)
    assertEquals(Nil, res.subvalues)
    assertEquals(None, res.prob)
  }

  @Test def testEmptyGBM(): Unit = {
    val res = AbaloneGBMModel(Abalone.getDefaultInstance)

    assertEquals(Some(0.5122293f), res.value)  // Some arbitrary value.
    assertEquals(Nil, res.errorMsgs)
    assertEquals(AbaloneGBMModelFeatures, res.missingVarNames)
    assertEquals(Nil, res.subvalues)
    assertEquals(None, res.prob)
  }

  @Test def testExternalResource(): Unit = {

    val json = """
                 |{
                 |  "modelType": "H2o",
                 |  "modelId": { "id": 0, "name": "" },
                 |  "features": {
                 |    "Sex":            { "type": "string", "spec": "0" },
                 |    "Length":         "1",
                 |    "Diameter":       "2",
                 |    "Height":         "3",
                 |    "Whole weight":   "4",
                 |    "Shucked weight": "5",
                 |    "Viscera weight": "6",
                 |    "Shell weight":   "7"
                 |  },
                 |  "modelUrl": "res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
                 |}
               """.stripMargin.trim

    val model = factory.fromString(json).get

    val padding: Seq[Option[H2oColumn]] = IndexedSeq(0, 0, 0, 0, 0, 0, 0)

    // These values are based on
    val values = sci.ListMap("M" -> 3.9671099427, "F" -> 3.9185206059454405, "I" -> 3.0928006453)

    values.foreach{ case (sex, exp) =>
      val out = model(string2col(sex) +: padding).value
      out.fold(fail(s"for $sex expected a result"))(assertEquals(s"for $sex", exp, _, 1.0e-6))
    }
  }

  @Test def testNotesAppear(): Unit = {
    val spec = Vfs.fromVfsType(VfsType.vfs2)("res:com/eharmony/aloha/models/h2o/test_spec.json")
    val model = Vfs.fromVfsType(VfsType.vfs2)("res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java")
    val notes = Option(Vector("this is a note", "another note"))
    val jsValue = H2oModel.json(spec, model, ModelId(1, "test-model"), None, externalModel = true, None, notes)
    val fields = jsValue.asJsObject.fields
    assertEquals(notes, fields.get("notes").map(_.convertTo[Vector[String]]))
  }

  @Test def removeLabel(): Unit = {
    val spec = Vfs.fromVfsType(VfsType.vfs2)("res:com/eharmony/aloha/models/h2o/test_spec.json")
    val model = Vfs.fromVfsType(VfsType.vfs2)("res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java")
    val jsValue = H2oModel.json(spec, model, ModelId(1, "test-model"), Option("Sex"), externalModel = true)
    val modelFeatures = jsValue.asJsObject.fields("features").asJsObject.fields
    assertFalse(modelFeatures.contains("Sex"))
  }

  @Test def testMissingNonCategorical(): Unit = {

    val json = """
                 |{
                 |  "modelType": "H2o",
                 |  "modelId": { "id": 0, "name": "" },
                 |  "features": {
                 |    "Sex":            { "type": "string", "spec": "0" },
                 |    "Length":         "1",
                 |    "Diameter":       "2",
                 |    "Height":         "3",
                 |    "Whole weight":   "4",
                 |    "Shucked weight": "5",
                 |    "Viscera weight": "6"
                 |  },
                 |  "modelUrl": "res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
                 |}
               """.stripMargin.trim

    val model = factory.fromString(json).get

    val padding: Seq[Option[H2oColumn]] = IndexedSeq(0, 0, 0, 0, 0, 0)

    val out = model(string2col("M") +: padding)

    val expected = RootedTreeImpl(ModelId(), sci.Seq("Ill-conditioned scalar prediction: NaN."), Set.empty, None, Nil, None)

    assertEquals(expected, out)
  }


  @Test def testNoFeatures(): Unit = {

    val json = """
                 |{
                 |  "modelType": "H2o",
                 |  "modelId": { "id": 0, "name": "no features h2o" },
                 |  "features": {},
                 |  "modelUrl": "res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
                 |}
               """.stripMargin.trim

    val model = factory.fromString(json).get

    try {
      model(Seq.empty)
    }
    catch {
      case e: IllegalArgumentException if e.getMessage.toLowerCase == MissingCategoricalMsg =>
      case e: Throwable => throw e
    }
  }

  @Test def testCategoricalMissing(): Unit = {

    val json = """
                 |{
                 |  "modelType": "H2o",
                 |  "modelId": { "id": 0, "name": "no features h2o" },
                 |  "features": {
                 |    "Sex": { "type": "string", "spec": "0" }
                 |  },
                 |  "modelUrl": "res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
                 |}
               """.stripMargin.trim

    val model = factory.fromString(json).get
    val padding: Seq[Option[H2oColumn]] = IndexedSeq(0, 0, 0, 0, 0, 0, 0)
    val input = Option(H2oMissingStringColumn) +: padding
    val out = model(input)

    val expected = RootedTreeImpl(
      ModelId(0, "no features h2o"),
      sci.Seq(
        "H2o model may have encountered a missing categorical variable.  Likely features: Sex",
        "See: glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.score0(glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java:59)"
      ),
      Set.empty, None, Nil, None
    )

    assertEquals(expected, out)
  }
}

object H2oModelTest {
  private val MissingCategoricalMsg = "categorical value out of range"

  private val Epsilon = 0.00001

  private lazy val protoSemantics = {
    val plugin = CompiledSemanticsProtoPlugin[Abalone]
    val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))
    val imports = Seq("scala.math._", "com.eharmony.aloha.feature.BasicFunctions._")
    val semantics = CompiledSemantics(compiler, plugin, imports)
    semantics
  }

  private def ProtoFactory[N: RefInfo]: ModelFactory[Abalone, NubRootedTree[N]] =
   ModelFactory.defaultFactory(protoSemantics, RootedTreeAuditor.noUpperBound[N]())

  /**
    * Recreate the h2o model results
    */

  private def ExpectedAbaloneModelResults: Seq[Double] = {
    val wts = Seq(
      0.0,-0.8257199606345127,0.048589336710687686,0.0,10.257730318325152,10.905035426114544,
      6.411898751852763,-17.066561775662798,-7.706232264683495,11.591721984154416,3.9185206059454405)

    val intercept = wts.last

    val is = VFS.getManager.resolveFile("res:abalone.csv").getContent.getInputStream
    scala.io.Source.fromInputStream(is).getLines.map { s =>
      val l = s split ","
      val z = l(0) match {
        case "M" => wts(2)
        case "I" => wts(1)
        case "F" => wts.head
      }
      l.slice(1, 8).zipWithIndex.foldLeft(z + intercept)((s, x) => s + x._1.toDouble * wts(3 + x._2))
    }.toList
  }

  private def  AbaloneData: Seq[Abalone] = {
    val is = VFS.getManager.resolveFile("res:abalone.csv").getContent.getInputStream
    scala.io.Source.fromInputStream(is).getLines.map { l =>
      val fields = l.split(",", -1)
      val b = Abalone.newBuilder()
      val w = Abalone.Weights.newBuilder()

      fields(0) match {
        case "M" => b setSex MALE
        case "F" => b setSex FEMALE
        case "I" => b setSex INFANT
      }

      fields(1).f.foreach(b.setLength)
      fields(2).f.foreach(b.setDiameter)
      fields(3).f.foreach(b.setHeight)
      fields(4).f.foreach(w.setWhole)
      fields(5).f.foreach(w.setShucked)
      fields(6).d.foreach(w.setViscera)
      fields(7).d.foreach(w.setShell)
      b.setWeight(w)
      b.build
    }.toList
  }

  private implicit class StringOps(val s: String) {
    def d: Option[Double] = try Option(s.toDouble) catch { case e: NumberFormatException => None}
    def f: Option[Float] = try Option(s.toFloat)  catch { case e: NumberFormatException => None}
  }

  sealed trait H2oColumn
  case class H2oStringColumn(value: String) extends H2oColumn
  case object H2oMissingStringColumn extends H2oColumn
  case class H2oDoubleColumn(value: Double) extends H2oColumn
  lazy val semantics = new Semantics[Seq[Option[H2oColumn]]] {
    override def refInfoA: RefInfo[Seq[Option[H2oColumn]]] = RefInfo[Seq[Option[H2oColumn]]]
    override def accessorFunctionNames: Seq[String] = Nil
    override def close(): Unit = ()
    override def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Seq[Option[H2oColumn]], B]] = {
      val i = codeSpec.toInt
      val f = (s: Seq[Option[H2oColumn]]) => Try {s(i)}.toOption.flatten match {
        case Some(H2oDoubleColumn(v)) => Some(v).asInstanceOf[B]
        case Some(H2oStringColumn(v)) => Some(v).asInstanceOf[B]
        case Some(H2oMissingStringColumn) => None.asInstanceOf[B]
        case d                        => d.asInstanceOf[B]
      }

      Right(GenFunc.f0(codeSpec, f))
    }
  }

  private lazy val factory = ModelFactory.defaultFactory(semantics, RootedTreeAuditor.noUpperBound[Double]())

  implicit def int2col(c: Int): Option[H2oColumn] = Option(H2oDoubleColumn(c))
  implicit def double2col(c: Double): Option[H2oColumn] = Option(H2oDoubleColumn(c))
  implicit def string2col(c: String): Option[H2oColumn] = Option(H2oStringColumn(c))


  // All of the features just return the value one would expect to be associated with the name.
  // The feature functions just exploit mathematical identities to show the expressive power of
  // the language.
  private val AbaloneModelJson =
    """
      |{
      |  "modelType": "H2o",
      |  "modelId": { "id": 0, "name": "proto model" },
      |  "features": {
      |    "Sex":            { "type": "string", "spec": "${sex}.name.substring(0,1)" },
      |    "Length":         "1d + ${length} - 1L",
      |    "Diameter":       "${diameter} * 1f",
      |    "Height":         "identity(${height})",
      |    "Whole weight":   "${weight.whole} * ${height} / ${height}",
      |    "Shucked weight": "pow(${weight.shucked}, 1)",
      |    "Viscera weight": "${weight.viscera} * (pow(sin(${diameter}), 2) + pow(cos(${diameter}), 2))",
      |    "Shell weight":   "${weight.shell} + log((${length} + ${height}) / (${height} + ${length}))",
      |
      |    "Circumference (unused)":  "Pi * ${diameter}"
      |  },
      |  "modelUrl": "res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
      |}
    """.stripMargin.trim

  private val AbaloneVecModelJson = {
    val featureStrs = Seq(
      "1d + ${length} - 1L",
      "${diameter} * 1f",
      "identity(${height})",
      "${weight.whole} * ${height} / ${height}",
      "pow(${weight.shucked}, 1)",
      "${weight.viscera} * (pow(sin(${diameter}), 2) + pow(cos(${diameter}), 2))"
    )

    val featureVec = featureStrs.mkString("Vector[Double](", ", ", ")")

    val prefix =
      """
        |{
        |  "modelType": "H2o",
        |  "modelId": { "id": 0, "name": "proto model" },
        |  "features": {
        |    "Sex":            { "type": "string", "spec": "${sex}.name.substring(0,1)" },
      """.stripMargin

    val features =
      s"""  "FeatureVec":     { "type": "double", "size": ${featureStrs.size}, "spec": "$featureVec" },
       """.stripMargin

    val suffix =
      """
        |    "Shell weight": "${weight.shell} + log((${length} + ${height}) / (${height} + ${length}))",
        |    "Circumference (unused)":  "Pi * ${diameter}"
        |  },
        |  "modelUrl": "res:com/eharmony/aloha/models/h2o/vec_glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java"
        |}
      """.stripMargin.trim

    prefix + features + suffix
  }


  private lazy val AbaloneModelFeatures =
    """\$\{([a-zA-Z\.]+)\}""".r.findAllMatchIn(AbaloneModelJson).map(_.group(1)).toSet

  private lazy val AbaloneModel: Model[Abalone, NubRootedTree[Float]] =
    ProtoFactory[Float].fromString(AbaloneModelJson).get

  private lazy val AbaloneVecModel: Model[Abalone, NubRootedTree[Float]] =
    ProtoFactory[Float].fromString(AbaloneVecModelJson).get


  private val AbaloneGBMModelJson =
    """
      |{
      |  "modelType": "H2o",
      |  "modelId": { "id": 0, "name": "proto model" },
      |  "features": {
      |    "Length":         "1d + ${length} - 1L",
      |    "Diameter":       "${diameter} * 1f",
      |    "Height":         "identity(${height})",
      |    "Whole weight":   "${weight.whole} * ${height} / ${height}",
      |    "Shucked weight": "pow(${weight.shucked}, 1)"
      |  },
      |  "modelUrl": "res:com/eharmony/aloha/models/h2o/gbm_00ba3eb1_288d_4c30_ad67_c69293b96efc.java"
      |}
    """.stripMargin.trim

  private lazy val AbaloneGBMModelFeatures =
    """\$\{([a-zA-Z\.]+)\}""".r.findAllMatchIn(AbaloneGBMModelJson).map(_.group(1)).toSet

  private lazy val AbaloneGBMModel: Model[Abalone, NubRootedTree[Float]] = ProtoFactory[Float].fromString(AbaloneGBMModelJson).get
}
