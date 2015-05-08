package com.eharmony.matching.featureSpecExtractor.csv.json

/*
TODO: Fix.  Figure out the required / vs optional variable accessors issue.
Document why need to wrap in option in wrappedSpec.
*/

import com.eharmony.matching.aloha.reflect.{RefInfoOps, RefInfo}
import spray.json._

import scala.collection.{immutable => sci}

sealed trait CsvColumnSpec {
    type ColType
    def name: String
    def spec: String
    def wrappedSpec = s"Option($spec)" // TODO: tell why this is necessary.
    def defVal: Option[ColType]
    def refInfo: RefInfo[Option[ColType]]
    def finalizer(nullString: String): Option[ColType] => String
}

case class CsvJson1(imports: Seq[String], features: sci.IndexedSeq[CsvColumnSpec])

object CsvJson1 extends DefaultJsonProtocol {
    implicit val csvJson1Format: RootJsonFormat[CsvJson1] = jsonFormat2(CsvJson1.apply)
}

object CsvColumnSpec extends DefaultJsonProtocol {
    private[this] implicit val stringCsvColumnSpecFormat: RootJsonFormat[StringCsvColumnSpec] = jsonFormat3(StringCsvColumnSpec.apply)
    private[this] implicit val doubleCsvColumnSpecFormat: RootJsonFormat[DoubleCsvColumnSpec] = jsonFormat3(DoubleCsvColumnSpec.apply)
    private[this] implicit val longCsvColumnSpecFormat: RootJsonFormat[LongCsvColumnSpec] = jsonFormat3(LongCsvColumnSpec.apply)

    // Need to for some reason name the fields explicitly here...
    private[this] implicit val enumCsvColumnSpecFormat: RootJsonFormat[EnumCsvColumnSpec] = jsonFormat(EnumCsvColumnSpec.apply, "name", "spec", "enumClass")
    private[this] implicit val syntheticEnumCsvColumnSpecFormat: RootJsonFormat[SyntheticEnumCsvColumnSpec] = jsonFormat(SyntheticEnumCsvColumnSpec.apply, "name", "spec", "values", "defVal")

    implicit val csvColumnSpecFormat: JsonFormat[CsvColumnSpec] = lift(new JsonReader[CsvColumnSpec] {
        def read(j: JsValue): CsvColumnSpec = {
            val o = j.asJsObject
            val fieldType = o.getFields("type").collectFirst { case JsString(s) => s.toLowerCase }
            val spec = fieldType match {
                case Some("string") => o.convertTo[StringCsvColumnSpec]
                case Some("double") => o.convertTo[DoubleCsvColumnSpec]
                case Some("long") => o.convertTo[LongCsvColumnSpec]
                case Some("enum") if o.getFields("enumClass").exists { case JsString(_) => true; case _ => false } =>
                    o.convertTo[EnumCsvColumnSpec]
                case Some("enum") if o.getFields("values").exists { case JsArray(_) => true; case _ => false } =>
                    o.convertTo[SyntheticEnumCsvColumnSpec]
                case None => deserializationError("No CSV field 'type' provided")
            }
            spec
        }
    })
}

final case class StringCsvColumnSpec(name: String, spec: String, defVal: Option[String] = None) extends CsvColumnSpec {

    type ColType = String
    def refInfo = RefInfo[Option[ColType]]
    def finalizer(nullString: String) = (s: Option[String]) => s.getOrElse(nullString)
}


final case class DoubleCsvColumnSpec(name: String, spec: String, defVal: Option[Double] = None) extends CsvColumnSpec {

    type ColType = Double
    def refInfo = RefInfo[Option[ColType]]
    def finalizer(nullString: String) = (s: Option[Double]) => s.map(_.toString).getOrElse(nullString)
}

final case class LongCsvColumnSpec(name: String, spec: String, defVal: Option[Long] = None) extends CsvColumnSpec {

    type ColType = Long
    def refInfo = RefInfo[Option[ColType]]
    def finalizer(nullString: String) = (s: Option[Long]) => s.map(_.toString).getOrElse(nullString)
}

final case class EnumCsvColumnSpec(name: String, spec: String, enumClass: String) extends CsvColumnSpec {

    /**
     * This may throw during the constructor call.  That's the correct time to throw.
     */
    private[this] val clazz = Class.forName(enumClass).asInstanceOf[Class[Enum[_]]]
//    private[this] val valSet = clazz.getEnumConstants.map(_.toString).toSet
    type ColType = Enum[_]
    def refInfo = RefInfoOps.option(RefInfoOps.fromSimpleClass(clazz))
    def finalizer(nullString: String) = (s: Option[Enum[_]]) => s.map(_.name).getOrElse(nullString)
    def defVal: Option[Enum[_]] = None
}

final case class SyntheticEnumCsvColumnSpec(name: String, spec: String, values: Seq[String], defVal: Option[String] = None) extends CsvColumnSpec {

    type ColType = String
    def refInfo = RefInfo[Option[ColType]]
    private[this] val valSet = values.toSet
    def finalizer(nullString: String) = (s: Option[String]) => s.filter(valSet.contains).getOrElse(nullString)
}
