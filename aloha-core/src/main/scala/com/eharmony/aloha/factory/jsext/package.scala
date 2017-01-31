package com.eharmony.aloha.factory

import spray.json._
import scala.collection.immutable.Seq

package object jsext {
  protected[aloha] implicit class JsValueExtensions(json: JsValue) {
    @inline def apply(fieldName: String): Option[JsValue] = json.asJsObject.getFields(fieldName).headOption
    @inline def sa(fieldName: String): Option[Seq[String]] = apply(fieldName) flatMap {
      case JsArray(a) =>
        val b = a collect {case JsString(s) => s}
        if (a.size == b.size) Some(b.toSeq) else None
      case _ => None
    }

    @inline def s(fieldName: String): Option[String] = apply(fieldName) collect {case JsString(s) => s}
    @inline def n(fieldName: String): Option[BigDecimal] = apply(fieldName) collect {case JsNumber(s) => s}
    @inline def b(fieldName: String): Option[Boolean] = apply(fieldName) collect {case JsBoolean(b) => b}
  }
}
