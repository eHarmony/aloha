package com.eharmony.aloha.semantics.compiled.plugin.csv

import spray.json._

import scala.util.{Failure, Try}


trait CsvProtocol {
    import DefaultJsonProtocol._

    private[this] sealed trait CsvColumn {
        def name: String
        def columnType: CsvTypes.CsvType
    }

    private[this] case class PrimitiveColumn(name: String, columnType: CsvTypes.CsvType) extends CsvColumn
    private[this] case class EnumColumn(name: String, columnType: CsvTypes.CsvType, enum: Enum) extends CsvColumn

    private[this] implicit object CsvColumnFormat extends JsonFormat[CsvColumn] {

        private[this] def nameToFieldOpt(name: String) = Option("name" -> JsString(name))

        override def write(c: CsvColumn): JsValue = {
            val t = Some("type" -> JsString(c.columnType.baseTypeString.toLowerCase))
            val o = Option(!c.columnType.isRequired).collect{ case true => "optional" -> JsBoolean(true)}
            val v = Option(c.columnType.isVectorized).collect{ case true => "vectorized" -> JsBoolean(true)}

            c match {
                case PrimitiveColumn(name, tpe)  => JsObject(Seq(nameToFieldOpt(name), t, o, v).flatten:_*)
                case EnumColumn(name, tpe, enum) =>
                    val cn = Some("className" -> JsString(enum.className))
                    val vs = Some("values" -> JsArray(enum.values().sortWith(_.ordinal < _.ordinal).map(_.toJson).toVector))
                    JsObject(Seq(nameToFieldOpt(name), t, o, v, cn, vs).flatten:_*)
            }
        }

        override def read(json: JsValue) = {
            val obj = json.asJsObject("Attempt to create CsvColumn but JSON was not an object")

            val nameAndType = obj.getFields("name", "type") match {
                case Seq(JsString(name), JsString(tpe)) => Option((name, tpe.capitalize))
                case _ => None
            }

            nameAndType map { case (name, tpe) =>
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
                    PrimitiveColumn(name, csvType)
                else {
                    val cn = (obj.getFields("className") match {
                        case Seq(JsString(c)) => Option(c)
                        case _ => None
                    }) getOrElse {
                        throw new DeserializationException("classname omitted in Enum column.")
                    }

                    val enum = obj.getFields("values").headOption map { vs =>
                        Try { Enum(cn, vs.convertTo[Vector[EnumConstant]].sortWith(_.ordinal < _.ordinal).map(c => (c.name(), c.getNumber())):_*) }.
                            recoverWith { case _ => Try { Enum.withNoNumbers(cn, vs.convertTo[Vector[String]]:_*) } }.
                            recoverWith { case _ => Failure(new DeserializationException("Couldn't get enum values.")) }.
                            get
                    } getOrElse { throw new DeserializationException("values not present for Enum.") }
                    EnumColumn(name, csvType, enum)
                }
            } getOrElse {
                throw new DeserializationException("name or type wasn't provided")
            }
        }
    }


    private[this] case class CsvJson(columns: Vector[CsvColumn],
                                     fs: Option[String],
                                     ifs: Option[String],
                                     missingData: Option[String],
                                     errorOnOptMissingField: Option[Boolean],
                                     errorOnOptMissingEnum: Option[Boolean]) extends CsvDataRetriever {

        def csvLines: CsvLines = {
            val ind = columns.map(_.name).zipWithIndex.toMap
            val enums = columns.collect { case EnumColumn(name, tpe, enum) => name -> enum}.toMap
            val m = missingData getOrElse ""
            val missing = (s: String) => s == m
            CsvLines(ind, enums, fs getOrElse "\t" , ifs getOrElse ",", missing, errorOnOptMissingField getOrElse false, errorOnOptMissingEnum getOrElse false)
        }

        def plugin: CompiledSemanticsCsvPlugin = {
            // TODO: make sure this is a hashmap, not a listmap.
            CompiledSemanticsCsvPlugin(columns.map {case c => (c.name, c.columnType)}.toMap)
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

