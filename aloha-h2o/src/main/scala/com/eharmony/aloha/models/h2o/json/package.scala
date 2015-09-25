package com.eharmony.aloha.models.h2o

import com.eharmony.aloha.reflect.RefInfo
import spray.json._


/**
 * Created by deak on 9/18/15.
 */
package object json {
//  sealed trait Spec {
//    type A
//    implicit def refInfo: RefInfo[A]
//    def spec: String
//    def defVal: Option
//    def `type`: String = getClass.getSimpleName.replaceFirst("^(.*)Spec$", "")
//  }
//
//  object Spec {
//    implicit object specJsonFormat extends JsonFormat[Spec] {
//      override def read(json: JsValue): Spec = {
//
//        json match {
//          case JsString(spec) => DoubleSpec(spec, None)
//          case JsObject(fields) =>
//            fields.contains()
//            fields.get("type") match {
//              case Some(JsString(t)) => t match {
//                case "String" =>
//              }
//              case _ => throw new DeserializationException("")
//            }
//
//        }
//
//
//      }
//
//      override def write(obj: Spec): JsValue = ???
//    }
//  }
//
//  case class StringSpec(spec: String, defVal: Option[String]) extends Spec {
//    type A = String
//    implicit def refInfo = RefInfo[String]
//  }
//
//  case class DoubleSpec(spec: String, defVal: Option[Double]) extends Spec {
//    type A = Double
//    implicit def refInfo = RefInfo[Double]
//  }
//


//  trait SpecJson {
//
//    protected[this] final val specJsonFormat = jsonFormat2(Spec)
//
//    protected[this] implicit object FeatureSpecFormat extends JsonFormat[Spec] {
//      def read(json: JsValue) = json match {
//        case JsString(s) => Spec(s)
//        case o: JsObject => o.convertTo[Spec](specJsonFormat)
//        case e => throw new DeserializationException(s"unexpected feature $e")
//      }
//
//      def write(spec: Spec) = spec.defVal.map {
//        case s if s.nonEmpty => JsObject(Map("spec" -> JsString(spec.spec), "defVal" -> s.toJson))
//        case s if s.isEmpty => JsString(spec.spec)
//      } getOrElse { JsString(spec.spec) }
//    }
//  }

}
