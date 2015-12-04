package com.eharmony.aloha.models.h2o

import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc}
import org.junit.{Ignore, Test}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol._
import org.junit.Assert._
import scala.collection.{immutable => sci}

import scala.language.implicitConversions
import scala.util.Try

/**
 * Created by deak on 10/23/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class H2oModelTest {
  import H2oModelTest._
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
