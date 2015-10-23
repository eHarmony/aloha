package com.eharmony.aloha.models.h2o.json


import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.h2o.{StringFeatureFunction, DoubleFeatureFunction, FeatureFunction}
import com.eharmony.aloha.reflect.{RefInfoOps, RefInfo}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.DefaultJsonProtocol._
import spray.json._
import com.eharmony.aloha.factory.ScalaJsonFormats.listMapFormat
import java.{lang => jl}

import scala.collection.immutable.ListMap
import scala.collection.{immutable => sci}

sealed trait H2oSpec {
  type A
  def name: String
  def spec: String
  def defVal: Option[A]
  implicit def refInfo: RefInfo[A]
  def ffConverter[B]: GenAggFunc[B, Option[A]] => FeatureFunction[B]

  def compile[B](semantics: Semantics[B]): Either[Seq[String], FeatureFunction[B]] =
    semantics.createFunction[Option[A]](spec, Option(defVal))(RefInfoOps.option[A]).right.map(ffConverter)
}

object H2oSpec {
  implicit val h2oSpecJsonFormat = lift(new RootJsonReader[H2oSpec] {
    override def read(json: JsValue): H2oSpec = {
      val jso = json.asJsObject
      jso.fields.get("type") match {
        case Some(JsString("double")) => jso.convertTo(jsonFormat3(DoubleH2oSpec))
        case Some(JsString("string")) => jso.convertTo(jsonFormat3(StringH2oSpec))
        case Some(JsString(d))        => throw new DeserializationException(s"unsupported H2oSpec type: $d. Should be 'double' or 'string'.")
        case Some(d)                  => throw new DeserializationException(s"H2oSpec type expected string, got: $d")
        case _                        => throw new DeserializationException(s"No 'type' field present.")
      }
    }
  })

  implicit val h2oFeaturesJsonFormat = lift(new RootJsonReader[sci.ListMap[String, H2oSpec]] with DefaultJsonProtocol {
    override def read(json: JsValue): sci.ListMap[String, H2oSpec] = {
      val m = json.convertTo[sci.ListMap[String, JsValue]]
      m.map {
        case (k, JsString(s)) => (k, DoubleH2oSpec(k, s, None))
        case (k, o: JsObject) => o.fields.get("type") match {
          case Some(JsString("double")) => (k, DoubleH2oSpec(k, spec(o), o.fields.get("defVal").flatMap(_.convertTo[Option[Double]])))
          case Some(JsString("string")) => (k, StringH2oSpec(k, spec(o), o.fields.get("defVal").flatMap(_.convertTo[Option[String]])))
          case Some(JsString(d))        => throw new DeserializationException(s"unsupported H2oSpec type: $d. Should be 'double' or 'string'.")
          case Some(d)                  => throw new DeserializationException(s"H2oSpec type expected string, got: $d")
          case _                        => throw new DeserializationException(s"No 'type' field present.")
        }
        case (k, v) => throw new DeserializationException(s"key '$k' needs to be a JSON string or object. found $v.")
      }
    }
    def spec(o: JsObject) = o.fields.get("spec").map(_.convertTo[String]).getOrElse(throw new DeserializationException("no string called 'spec'."))
  })
}

case class DoubleH2oSpec(name: String, spec: String, defVal: Option[Double]) extends H2oSpec {
  type A = Double
  def ffConverter[B] = f => DoubleFeatureFunction(f.andThenGenAggFunc(_.map(v => jl.Double.valueOf(v))))
  def refInfo = RefInfo[Double]
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
                  numMissingThreshold: Option[Int] = None)

/**
 * Created by deak on 10/22/15.
 */
private[h2o] trait H2oModelJson {

  implicit val h2oAstJsonFormat = new RootJsonReader[H2oAst] {
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

      H2oAst(modelType, modelId, modelSource, features, numMissingThreshold)
    }
  }
}
