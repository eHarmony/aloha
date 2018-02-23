package com.eharmony.aloha.models.h2o.json


import java.{lang => jl}

import com.eharmony.aloha.factory.ScalaJsonFormats.listMapFormat
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.h2o.{DoubleFeatureFunction, DoubleSeqFeatureFunction, FeatureFunction, StringFeatureFunction}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.immutable.ListMap
import scala.collection.{immutable => sci}

sealed trait H2oSpec {
  type A
  def name: String
  def spec: String

  /**
    * This will be used as a default if the resulting function returns `None` for a given value.
    * @return
    */
  def defVal: Option[A]
  implicit def refInfo: RefInfo[A]
  def ffConverter[B]: GenAggFunc[B, Option[A]] => FeatureFunction[B]

  def compile[B](semantics: Semantics[B]): Either[Seq[String], FeatureFunction[B]] =
    semantics.createFunction[Option[A]](spec, Option(defVal))(RefInfoOps.option[A]).right.map(f =>
      ffConverter(f.andThenGenAggFunc(_ orElse defVal)))
}

object H2oSpec {
  // Used in CLI.
  private[h2o] implicit val h2oSpecJsonFormat = lift(new RootJsonReader[H2oSpec] {
    override def read(json: JsValue): H2oSpec = {
      val jso = json.asJsObject
      jso.fields.get("type") match {
        case None                     => jso.convertTo(jsonFormat3(DoubleH2oSpec)) // Default is double type.
        case Some(JsString("double")) if jso.fields.contains("size") =>
          jso.convertTo(jsonFormat4(DoubleSeqH2oSpec))
        case Some(JsString("double")) =>
          jso.convertTo(jsonFormat3(DoubleH2oSpec))
        case Some(JsString("string")) => jso.convertTo(jsonFormat3(StringH2oSpec))
        case Some(JsString(t))        => throw new DeserializationException(s"unsupported H2oSpec type: $t. Should be 'double' or 'string'.")
        case Some(t)                  => throw new DeserializationException(s"H2oSpec type expected string, got: $t")
      }
    }
  })

  private[h2o] implicit val h2oFeaturesJsonFormat = new RootJsonFormat[sci.ListMap[String, H2oSpec]] with DefaultJsonProtocol {
    override def read(json: JsValue): sci.ListMap[String, H2oSpec] = {
      val m = json.convertTo[sci.ListMap[String, JsValue]]
      m.map {
        case (k, JsString(s)) => (k, DoubleH2oSpec(k, s, None))
        case (k, o: JsObject) => o.fields.get("type") match {
          case Some(JsString("double")) if o.fields.contains("size") =>
            (k, DoubleSeqH2oSpec(k, spec(o), size(o), o.fields.get("defVal").flatMap(_.convertTo[Option[Seq[Double]]])))
          case None | Some(JsString("double")) => (k, DoubleH2oSpec(k, spec(o), o.fields.get("defVal").flatMap(_.convertTo[Option[Double]])))
          case Some(JsString("string"))        => (k, StringH2oSpec(k, spec(o), o.fields.get("defVal").flatMap(_.convertTo[Option[String]])))
          case Some(JsString(d))               => throw new DeserializationException(s"unsupported H2oSpec type: $d. Should be 'double' or 'string'.")
          case Some(d)                         => throw new DeserializationException(s"H2oSpec type expected string, got: $d")
        }
        case (k, v) => throw new DeserializationException(s"key '$k' needs to be a JSON string or object. found $v.")
      }
    }

    override def write(features: sci.ListMap[String, H2oSpec]): JsValue = {
      def dd(s: DoubleH2oSpec) = s.defVal.map(d => Map("defVal" -> JsNumber(d))).getOrElse(Map.empty)
      def ddn(s: DoubleSeqH2oSpec) = s.defVal.map{ d =>
        val vec: Vector[JsValue] = d.map(x => JsNumber(x))(scala.collection.breakOut)
        Map("defVal" -> JsArray(vec))
      }.getOrElse(Map.empty)
      def ds(s: StringH2oSpec) = s.defVal.map(d => Map("defVal" -> JsString(d))).getOrElse(Map.empty)

      val fs = features.map {
        case (k, DoubleH2oSpec(name, spec, None)) => (k, JsString(spec))
        case (k, s: DoubleH2oSpec) => (k, JsObject(sci.ListMap[String, JsValue]("spec" -> JsString(s.spec)) ++ dd(s) ++ Seq("type" -> JsString("double"))))
        case (k, s: DoubleSeqH2oSpec) => (k, JsObject(sci.ListMap[String, JsValue]("spec" -> JsString(s.spec)) ++ ddn(s) ++ Seq("type" -> JsString("double"), "size" -> JsNumber(s.size))))
        case (k, s: StringH2oSpec) => (k, JsObject(sci.ListMap[String, JsValue]("spec" -> JsString(s.spec)) ++ ds(s) ++ Seq("type" -> JsString("string"))))
      }

      JsObject(fs)
    }

    def spec(o: JsObject) = o.fields.get("spec").map(_.convertTo[String]).getOrElse(throw new DeserializationException("no string called 'spec'."))
    def size(o: JsObject) = o.fields.get("size").map(_.convertTo[Int]).getOrElse(throw new DeserializationException("no int called 'size'."))
  }
}

case class DoubleH2oSpec(name: String, spec: String, defVal: Option[Double]) extends H2oSpec {
  type A = Double
  def ffConverter[B] = f => DoubleFeatureFunction(f.andThenGenAggFunc(_.map(v => jl.Double.valueOf(v))))
  def refInfo = RefInfo[Double]
}

case class DoubleSeqH2oSpec(name: String, spec: String, size: Int, defVal: Option[Seq[Double]]) extends H2oSpec {
  type A = Seq[Double]
  def ffConverter[B] = f => DoubleSeqFeatureFunction(f, size)
  def refInfo = RefInfo[Seq[Double]]

  protected def sizeErr: String = s"feature '$name' output size != $size"

  // NOTE: override here and wrap spec in Option to avoid adding implicit Option lift for Seq[Double]
  override def compile[B](semantics: Semantics[B]): Either[Seq[String], FeatureFunction[B]] = {
    val wrappedSpec = s"Option($spec).map{x => require(x.size == $size, " + s""""$sizeErr"); x}"""
    semantics.createFunction[Option[Seq[Double]]](wrappedSpec, Option(defVal))(RefInfo[Option[Seq[Double]]]).right.map(f =>
      ffConverter(f.andThenGenAggFunc(_ orElse defVal)))
  }
}

case class StringH2oSpec(name: String, spec: String, defVal: Option[String]) extends H2oSpec {
  type A = String
  def ffConverter[B] = StringFeatureFunction(_)
  def refInfo = RefInfo[String]
}

case class H2oAst(modelType: String,
                  modelId: ModelId,
                  modelSource: ModelSource,
                  features: sci.ListMap[String, H2oSpec],
                  numMissingThreshold: Option[Int] = None,
                  notes: Option[Seq[String]] = None)

private[h2o] object H2oAst {
  implicit val h2oAstJsonFormat = new RootJsonFormat[H2oAst] with DefaultJsonProtocol {
    override def read(json: JsValue): H2oAst = {
      val jso = json.asJsObject
      val modelSource = json.convertTo[ModelSource]
      val (modelType, modelId, features) = jso.getFields("modelType", "modelId", "features") match {
        case Seq(JsString(mt), mid, fs) =>
          (mt, mid.convertTo[ModelId], fs.convertTo(H2oSpec.h2oFeaturesJsonFormat))
        case _ => throw new DeserializationException("bad format")
      }
      val numMissingThreshold = jso.getFields("numMissingThreshold") match {
        case Seq(JsNumber(n)) => Option(n.toIntExact)
        case _ => None
      }

      val notes = jso.getFields("notes") match {
        case Seq(a: JsArray) => Option(a.convertTo[Seq[String]]).filter(_.nonEmpty)
        case _ => None
      }

      H2oAst(modelType, modelId, modelSource, features, numMissingThreshold, notes)
    }

    override def write(h2oAst: H2oAst): JsValue = {
      val fields = Seq("modelType" -> h2oAst.modelType.toJson) ++
                   Seq("modelId" -> h2oAst.modelId.toJson) ++
                   h2oAst.numMissingThreshold.map(t => "numMissingThreshold" -> t.toJson).toSeq ++
                   Seq("features" -> h2oAst.features.toJson(H2oSpec.h2oFeaturesJsonFormat)) ++
                   h2oAst.modelSource.toJson.asJsObject.fields.toSeq ++
                   h2oAst.notes.map(t => "notes" -> t.toJson).toSeq
      JsObject(ListMap(fields:_*))
    }
  }
}
