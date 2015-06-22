package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import spray.json._

import com.eharmony.matching.aloha.factory.ScalaJsonFormats.listMapFormat
import scala.collection.immutable.ListMap
import scala.util.{Failure, Try}


trait CsvProtocol {
    import DefaultJsonProtocol._

    private[this] sealed trait CsvColumn {
        def columnType: CsvTypes.CsvType
    }

    private[this] case class PrimitiveColumn(columnType: CsvTypes.CsvType) extends CsvColumn
    private[this] case class EnumColumn(columnType: CsvTypes.CsvType, enum: Enum) extends CsvColumn


    private[this] implicit object CsvColumnFormat extends JsonFormat[CsvColumn] {

        override def write(c: CsvColumn): JsValue = {
            val t = Some("type" -> JsString(c.columnType.baseTypeString.toLowerCase))
            val o = Option(!c.columnType.isRequired).collect{ case true => "optional" -> JsBoolean(true)}
            val v = Option(c.columnType.isVectorized).collect{ case true => "vectorized" -> JsBoolean(true)}

            c match {
                case PrimitiveColumn(tpe)  => JsObject(Seq(t, o, v).flatten:_*)
                case EnumColumn(tpe, enum) =>
                    val cn = Some("className" -> JsString(enum.className))
                    val vs = Some("values" -> JsObject(ListMap(enum.values().sortWith(_.ordinal() < _.ordinal()).map(c => c.name() -> JsNumber(c.getNumber())):_*)))
                    JsObject(Seq(t, o, v, cn, vs).flatten:_*)
            }
        }

        override def read(json: JsValue) = {
            val obj = json.asJsObject("Attempt to create CsvColumn but JSON was not an object")

            val t = obj.getFields("type") match {
                case Seq(JsString(tpe)) => Option(tpe.capitalize)
                case _ => None
            }

            t map { tpe =>
                val o = obj.getFields("optional") match {
                    case Seq(JsBoolean(b)) => b
                    case _ => false
                }

                val v = obj.getFields("vectorized") match {
                    case Seq(JsBoolean(b)) => b
                    case _ => false
                }
                val csvTypeString = (if (v) "v" else "") + (if (o) "o" else "") + tpe.toLowerCase.charAt(0)
                val csvType = CsvTypes.withNameExtended(csvTypeString)

                if (tpe != "Enum")
                    PrimitiveColumn(csvType)
                else {
                    val cn = (obj.getFields("className") match {
                        case Seq(JsString(c)) => Option(c)
                        case _ => None
                    }) getOrElse {
                        throw new DeserializationException("classname omitted in Enum column.")
                    }

                    val enum = obj.getFields("values").headOption map { vs =>
                        Try { Enum(cn, vs.convertTo[ListMap[String, Int]].toSeq:_*) }.
                            recoverWith { case _ => Try { Enum.withNoNumbers(cn, vs.convertTo[Vector[String]]:_*) } }.
                            recoverWith { case _ => Failure(new DeserializationException("Couldn't get enum values.")) }.
                            get
                    } getOrElse { throw new DeserializationException("values not present for Enum.") }
                    EnumColumn(csvType, enum)
                }
            } getOrElse {
                throw new DeserializationException("no type provided")
            }
        }
    }


    private[this] case class CsvJson(columns: ListMap[String, CsvColumn],
                       fs: Option[String],
                       ifs: Option[String],
                       missingData: Option[String],
                       errorOnOptMissingField: Option[Boolean],
                       errorOnOptMissingEnum: Option[Boolean]) extends CsvDataRetriever {

        def csvLines: CsvLines = {
            val ind = columns.unzip._1.zipWithIndex.toMap
            val enums = columns.collect { case (name, EnumColumn(tpe, enum)) => name -> enum}.toMap
            val m = missingData getOrElse ""
            val missing = (s: String) => s == m
            CsvLines(ind, enums, fs getOrElse "\t" , ifs getOrElse ",", missing, errorOnOptMissingField getOrElse false, errorOnOptMissingEnum getOrElse false)
        }

        def plugin: CompiledSemanticsCsvPlugin = {
            // TODO: make sure this is a hashmap, not a listmap.
            CompiledSemanticsCsvPlugin(columns.map {case (name, col) => (name, col.columnType)}.toMap)
        }
    }

    private[this] implicit val csvJsonFormat = jsonFormat6(CsvJson.apply)

    sealed trait CsvDataRetriever {
        def csvLines: CsvLines
        def plugin: CompiledSemanticsCsvPlugin
    }

    def getCsvDataRetriever(jsValue: JsValue): CsvDataRetriever = jsValue.convertTo[CsvJson]
}

object CsvProtocol extends CsvProtocol

