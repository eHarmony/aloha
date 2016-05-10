package com.eharmony.aloha.models.h2o

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.vfs.{VfsType, Vfs}
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.proto.CompiledSemanticsProtoPlugin
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc}
import com.eharmony.aloha.test.proto.TestProtoBuffs.Abalone
import com.eharmony.aloha.test.proto.TestProtoBuffs.Abalone.Gender.{FEMALE, MALE, INFANT}
import com.eharmony.aloha.util.Logging
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol._
import org.apache.commons.vfs2.VFS
import spray.json.JsonReader

import scala.collection.{immutable => sci}
import scala.language.implicitConversions
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by deak on 10/23/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class H2oModelTest extends Logging {
  import H2oModelTest._

  @Test def testProto(): Unit = {
    // All of the features just return the value one would expect to be associated with the name.
    // The feature functions just exploit mathematical identities to show the expressive power of
    // the language.

    val json =
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

    val model: Model[Abalone, Float] = ProtoFactory[Float].fromString(json).get

    // For expository purposes:
    val input:    Abalone       = AbaloneData.toStream.head
    val expected: Double        = ExpectedAbaloneModelResults.toStream.head
    val actual:   Option[Float] = model(input)
    assertEquals(expected, actual.get, Epsilon)


    // Test predictions are correct.
    // To test in parallel, do something like  // val data = Vector.fill(1000)(AbaloneData.toVector).flatten.par
    val data = ExpectedAbaloneModelResults.zip(AbaloneData).zipWithIndex
    data foreach { case ((exp, abalone), i) =>

      // The prediction loop:  predict, given a native input type of the caller's choosing.
      val act: Option[Float] = model(abalone)
      assertEquals(s"in test $i", exp, act.get, Epsilon)
    }
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
      val out = model(string2col(sex) +: padding)
      out.fold(fail(s"for $sex expected a result"))(assertEquals(s"for $sex", exp, _, 1.0e-6))
    }
  }

  @Test def testNotesAppear(): Unit = {

    val spec = Vfs.fromVfsType(VfsType.vfs2)("res:com/eharmony/aloha/models/h2o/test_spec.json")
    val model = Vfs.fromVfsType(VfsType.vfs2)("res:com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java")
    val notes = Option(Vector("this is a note", "another note"))
    val jsValue = H2oModel.json(spec, model, ModelId(1, "test-model"), true, None, notes)
    val fields = jsValue.asJsObject.fields
    assertEquals(notes, fields.get("notes").map(_.convertTo[Vector[String]]))
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

    val out = model.score(string2col("M") +: padding)

    val expected =
      """
        |error {
        |  model {
        |    id: 0
        |    name: ""
        |  }
        |  messages: "Ill-conditioned scalar prediction: NaN."
        |}
      """.stripMargin.trim

    assertEquals(expected, out.toString.trim)
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
    val out = model.score(Option(H2oMissingStringColumn) +: padding)

    val expected =
      """
        |error {
        |  model {
        |    id: 0
        |    name: "no features h2o"
        |  }
        |  missing_features {
        |    names: "0"
        |  }
        |  messages: "H2o model may have encountered a missing categorical variable.  Likely features: Sex"
        |  messages: "See: glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.score0(glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java:59)"
        |}
        |
      """.stripMargin.trim

    assertEquals(expected, out.toString.trim)
  }
}

object H2oModelTest {
  val MissingCategoricalMsg = "categorical value out of range"

  val Epsilon = 0.00001

  lazy val protoSemantics = {
    val plugin = CompiledSemanticsProtoPlugin[Abalone]
    val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))
    val imports = Seq("scala.math._", "com.eharmony.aloha.feature.BasicFunctions._")
    val semantics = CompiledSemantics(compiler, plugin, imports)
    semantics
  }

  def ProtoFactory[B: RefInfo: JsonReader: ScoreConverter] =
    ModelFactory.defaultFactory.toTypedFactory[Abalone, B](protoSemantics)

  /**
    * Recreate the h2o model results
    */

  def ExpectedAbaloneModelResults = {
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
        case "F" => wts(0)
      }
      l.slice(1, 8).zipWithIndex.foldLeft(z + intercept)((s, x) => s + x._1.toDouble * wts(3 + x._2))
    }
  }

  def AbaloneData: Iterator[Abalone] = {
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
    }
  }

  private implicit class StringOps(val s: String) {
    def d = try Option(s.toDouble) catch { case e: NumberFormatException => None}
    def f = try Option(s.toFloat)  catch { case e: NumberFormatException => None}
  }

  sealed trait H2oColumn
  case class H2oStringColumn(value: String) extends H2oColumn
  case object H2oMissingStringColumn extends H2oColumn
  case class H2oDoubleColumn(value: Double) extends H2oColumn
  lazy val semantics = new Semantics[Seq[Option[H2oColumn]]] {
    override def refInfoA = RefInfo[Seq[Option[H2oColumn]]]
    override def accessorFunctionNames: Seq[String] = Nil
    override def close(): Unit = ()
    override def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Seq[Option[H2oColumn]], B]] = {
      val b = RefInfo[B]
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

  lazy val factory = ModelFactory(H2oModel.parser).toTypedFactory[Seq[Option[H2oColumn]], Double](semantics)

  implicit def int2col(c: Int): Option[H2oColumn] = Option(H2oDoubleColumn(c))
  implicit def double2col(c: Double): Option[H2oColumn] = Option(H2oDoubleColumn(c))
  implicit def string2col(c: String): Option[H2oColumn] = Option(H2oStringColumn(c))
}
