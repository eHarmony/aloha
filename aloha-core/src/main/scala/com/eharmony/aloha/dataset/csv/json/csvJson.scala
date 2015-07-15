package com.eharmony.aloha.dataset.csv.json

/*
Document why need to wrap in option in wrappedSpec.
*/

import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicFinalizer, EncodingBasedFinalizer, Finalizer}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.util.Logging
import spray.json._

import scala.collection.{immutable => sci}
import scala.language.implicitConversions

sealed trait CsvColumn {
    type ColType
    def name: String
    def spec: String
    def wrappedSpec = s"Option($spec)"
    def defVal: Option[ColType]
    def refInfo: RefInfo[Option[ColType]]
    def finalizer(sep: String, nullString: String): Finalizer[ColType]
}

final case class CsvJson(
    imports: Seq[String],
    features: sci.IndexedSeq[CsvColumn],
    separator: Option[String],
    nullValue: Option[String],
    encoding: Option[Encoding]
)

object CsvJson extends DefaultJsonProtocol {
    implicit val csvJson1Format: RootJsonFormat[CsvJson] = jsonFormat5(CsvJson.apply)
}

object CsvColumn
extends DefaultJsonProtocol
   with Logging {
    private[this] implicit def csvColumnSpecWithDefaultFormat[A: RefInfo: JsonFormat]: RootJsonFormat[CsvColumnWithDefault[A]] = jsonFormat(CsvColumnWithDefault.apply[A], "name", "spec", "defVal")
    private[this] implicit val defaultCsvColumnSpecFormat: RootJsonFormat[DefaultCsvColumn] = jsonFormat(DefaultCsvColumn.apply, "name", "spec")

    // Need to for some reason name the fields explicitly here...
    private[this] implicit val enumCsvColumnSpecFormat: RootJsonFormat[EnumCsvColumn] = jsonFormat(EnumCsvColumn.apply, "name", "spec", "enumClass")
    private[this] implicit val syntheticEnumCsvColumnSpecFormat: RootJsonFormat[SyntheticEnumCsvColumn] = jsonFormat(SyntheticEnumCsvColumn.apply, "name", "spec", "values", "defVal")

    /**
     * Important.  If a type is not supplied, Double is assumed.
     */
    implicit val csvColumnSpecFormat: JsonFormat[CsvColumn] = lift(new JsonReader[CsvColumn] {
        def read(j: JsValue): CsvColumn = {
            val o = j.asJsObject("CSV Column should be an object")
            val fieldType = o.getFields("type") match {
                case Seq(JsString(s)) => Option(s.toLowerCase)
                case _ => None
            }

            val spec = fieldType match {
                case Some("string") => o.convertTo[CsvColumnWithDefault[String]]
                case Some("double") => o.convertTo[CsvColumnWithDefault[Double]]
                case Some("float") => o.convertTo[CsvColumnWithDefault[Float]]
                case Some("long") => o.convertTo[CsvColumnWithDefault[Long]]
                case Some("int") => o.convertTo[CsvColumnWithDefault[Int]]
                case Some("short") => o.convertTo[CsvColumnWithDefault[Short]]
                case Some("byte") => o.convertTo[CsvColumnWithDefault[Byte]]
                case Some("char") => o.convertTo[CsvColumnWithDefault[Char]]
                case Some("boolean") => o.convertTo[CsvColumnWithDefault[Boolean]]
                case Some("enum") if o.getFields("enumClass").exists { case JsString(_) => true; case _ => false } =>
                    o.convertTo[EnumCsvColumn]
                case Some("enum") if o.getFields("values").exists { case JsArray(_) => true; case _ => false } =>
                    o.convertTo[SyntheticEnumCsvColumn]
                case None =>
                    debug(s"No type provided.  Assuming Any.  Given: ${o.compactPrint}")
                    o.convertTo[DefaultCsvColumn]
            }
            spec
        }
    })
}

final case class CsvColumnWithDefault[C: RefInfo: JsonReader](name: String, spec: String, defVal: Option[C] = None) extends CsvColumn {
    type ColType = C
    val refInfo = RefInfoOps.option[C]
    def finalizer(sep: String, nullString: String) = BasicFinalizer(_.fold(nullString)(_.toString))
}

final case class DefaultCsvColumn(name: String, spec: String) extends CsvColumn {
    type ColType = Any
    def defVal: Option[ColType] = None
    val refInfo = RefInfo[Option[Any]]
    def finalizer(sep: String, nullString: String) = BasicFinalizer(_.fold(nullString)(_.toString))
}

final case class EnumCsvColumn(name: String, spec: String, enumClass: String)
extends CsvColumn {

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

final case class SyntheticEnumCsvColumn(name: String, spec: String, values: Seq[String], defVal: Option[String] = None)
extends CsvColumn {
    type ColType = String
    def refInfo = RefInfo[Option[ColType]]
    def finalizer(sep: String, nullString: String) = EncodingBasedFinalizer((e: Encoding) => e.finalizer(sep, nullString, values))
}
