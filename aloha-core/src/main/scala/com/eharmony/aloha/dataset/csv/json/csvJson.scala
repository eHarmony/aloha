package com.eharmony.aloha.dataset.csv.json

/*
TODO: Fix.  Figure out the required / vs optional variable accessors issue.
Document why need to wrap in option in wrappedSpec.
*/

import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicFinalizer, EncodingBasedFinalizer, Finalizer}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.util.Logging
import spray.json._

import scala.collection.{immutable => sci}
import scala.language.implicitConversions

sealed trait CsvColumnSpec {
    type ColType
    def name: String
    def spec: String
    def wrappedSpec = s"Option($spec)" // TODO: tell why this is necessary.
    def defVal: Option[ColType]
    def refInfo: RefInfo[Option[ColType]]
    def finalizer(sep: String, nullString: String): Finalizer[ColType]
}

final case class CsvJson(
    imports: Seq[String],
    features: sci.IndexedSeq[CsvColumnSpec],
    separator: Option[String],
    nullValue: Option[String],
    encoding: Option[Encoding]
)

object CsvJson extends DefaultJsonProtocol {
    implicit val csvJson1Format: RootJsonFormat[CsvJson] = jsonFormat5(CsvJson.apply)
}

object CsvColumnSpec
extends DefaultJsonProtocol
   with Logging {
    private[this] implicit def csvColumnSpecWithDefaultFormat[A: RefInfo: JsonFormat]: RootJsonFormat[CsvColumnSpecWithDefault[A]] = jsonFormat(CsvColumnSpecWithDefault.apply[A], "name", "spec", "defVal")
    private[this] implicit val defaultCsvColumnSpecFormat: RootJsonFormat[DefaultCsvColumnSpec] = jsonFormat(DefaultCsvColumnSpec.apply, "name", "spec")

    // Need to for some reason name the fields explicitly here...
    private[this] implicit val enumCsvColumnSpecFormat: RootJsonFormat[EnumCsvColumnSpec] = jsonFormat(EnumCsvColumnSpec.apply, "name", "spec", "enumClass")
    private[this] implicit val syntheticEnumCsvColumnSpecFormat: RootJsonFormat[SyntheticEnumCsvColumnSpec] = jsonFormat(SyntheticEnumCsvColumnSpec.apply, "name", "spec", "values", "defVal")

    /**
     * Important.  If a type is not supplied, Double is assumed.
     */
    implicit val csvColumnSpecFormat: JsonFormat[CsvColumnSpec] = lift(new JsonReader[CsvColumnSpec] {
        def read(j: JsValue): CsvColumnSpec = {
            val o = j.asJsObject
            val fieldType = o.getFields("type").collectFirst { case JsString(s) => s.toLowerCase }
            val spec = fieldType match {
                case Some("string") => o.convertTo[CsvColumnSpecWithDefault[String]]
                case Some("double") => o.convertTo[CsvColumnSpecWithDefault[Double]]
                case Some("float") => o.convertTo[CsvColumnSpecWithDefault[Float]]
                case Some("long") => o.convertTo[CsvColumnSpecWithDefault[Long]]
                case Some("int") => o.convertTo[CsvColumnSpecWithDefault[Int]]
                case Some("short") => o.convertTo[CsvColumnSpecWithDefault[Short]]
                case Some("byte") => o.convertTo[CsvColumnSpecWithDefault[Byte]]
                case Some("char") => o.convertTo[CsvColumnSpecWithDefault[Char]]
                case Some("boolean") => o.convertTo[CsvColumnSpecWithDefault[Boolean]]
                case Some("enum") if o.getFields("enumClass").exists { case JsString(_) => true; case _ => false } =>
                    o.convertTo[EnumCsvColumnSpec]
                case Some("enum") if o.getFields("values").exists { case JsArray(_) => true; case _ => false } =>
                    o.convertTo[SyntheticEnumCsvColumnSpec]
                case None =>
                    debug(s"No type provided.  Assuming Any.  Given: ${o.compactPrint}")
                    o.convertTo[DefaultCsvColumnSpec]
            }
            spec
        }
    })
}

final case class CsvColumnSpecWithDefault[C: RefInfo: JsonReader](name: String, spec: String, defVal: Option[C] = None) extends CsvColumnSpec {
    type ColType = C
    val refInfo = RefInfoOps.option[C]
    def finalizer(sep: String, nullString: String) = BasicFinalizer(_.fold(nullString)(_.toString))
}

final case class DefaultCsvColumnSpec(name: String, spec: String) extends CsvColumnSpec {
    type ColType = Any
    def defVal: Option[ColType] = None
    val refInfo = RefInfo[Option[Any]]
    def finalizer(sep: String, nullString: String) = BasicFinalizer(_.fold(nullString)(_.toString))
}

final case class EnumCsvColumnSpec(name: String, spec: String, enumClass: String)
extends CsvColumnSpec {

    /**
     * This may throw during the constructor call.  That's the correct time to throw.
     */
    type ColType = Enum[_]
    private[this] val clazz = Class.forName(enumClass).asInstanceOf[Class[Enum[_]]]
    private[this] def values = clazz.getEnumConstants.map(_.name)
    def refInfo = RefInfoOps.option(RefInfoOps.fromSimpleClass(clazz))
    def defVal: Option[Enum[_]] = None
    def finalizer(sep: String, nullString: String) = EncodingBasedFinalizer((e: Encoding) => e.finalizer(sep, nullString, values))
}

final case class SyntheticEnumCsvColumnSpec(name: String, spec: String, values: Seq[String], defVal: Option[String] = None)
extends CsvColumnSpec {
    type ColType = String
    def refInfo = RefInfo[Option[ColType]]
    def finalizer(sep: String, nullString: String) = EncodingBasedFinalizer((e: Encoding) => e.finalizer(sep, nullString, values))
}
