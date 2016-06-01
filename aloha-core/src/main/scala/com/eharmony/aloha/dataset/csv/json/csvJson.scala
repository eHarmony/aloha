package com.eharmony.aloha.dataset.csv.json

import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicFinalizer, EncodingBasedFinalizer, Finalizer}
import com.eharmony.aloha.factory.Formats
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.util.Logging
import spray.json._

import scala.collection.{immutable => sci}
import scala.language.implicitConversions
import scala.util.Try

/**
  * Note that the codomain of functions produced from a CsvColumn, `c`, is `Option[c.ColType]`.
  * This is so that functions with optional variables can be treated the same way as functions
  * with no variables or only required variables.  In order to accomplish this, we need to
  * {{{
  * val semantics: CompiledSemantics[A] = ???
  * val c: CsvColumn = ???
  *
  * val f = semantics.createFunction[Option[c.ColType]](c.wrappedSpec, Some(c.defVal))(c.refInfo)
  *
  * f match {
  *   case Right(success) => success.andThenGenAggFunc(_ orElse c.defVal)
  * }
  * }}}
  */
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
    private[this] implicit def optionCsvColumnSpecWithDefaultFormat[A: RefInfo: JsonFormat]: RootJsonFormat[OptionCsvColumnWithDefault[A]] = jsonFormat(OptionCsvColumnWithDefault.apply[A], "name", "spec", "defVal")
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

            val optional = o.getFields("optional") match {
                case Seq(JsBoolean(bool)) => bool
                case _ => false
            }

            lazy val enumClassName = o.getFields("enumClass") match {
                case Seq(JsString(c)) => Some(c)
                case _ => None
            }

            lazy val enumClass = enumClassName.flatMap(c =>
                Try { Class.forName(c) }.
                  toOption.
                  collect { case e if e.isEnum => e.asInstanceOf[Class[Enum[_]]] }
            )

            val spec = fieldType match {
                case Some("string")  if optional => o.convertTo[OptionCsvColumnWithDefault[String]]
                case Some("double")  if optional => o.convertTo[OptionCsvColumnWithDefault[Double]]
                case Some("float")   if optional => o.convertTo[OptionCsvColumnWithDefault[Float]]
                case Some("long")    if optional => o.convertTo[OptionCsvColumnWithDefault[Long]]
                case Some("int")     if optional => o.convertTo[OptionCsvColumnWithDefault[Int]]
                case Some("short")   if optional => o.convertTo[OptionCsvColumnWithDefault[Short]]
                case Some("byte")    if optional => o.convertTo[OptionCsvColumnWithDefault[Byte]]
                case Some("char")    if optional => o.convertTo[OptionCsvColumnWithDefault[Char]]
                case Some("boolean") if optional => o.convertTo[OptionCsvColumnWithDefault[Boolean]]

                case Some("string")  => o.convertTo[CsvColumnWithDefault[String]]
                case Some("double")  => o.convertTo[CsvColumnWithDefault[Double]]
                case Some("float")   => o.convertTo[CsvColumnWithDefault[Float]]
                case Some("long")    => o.convertTo[CsvColumnWithDefault[Long]]
                case Some("int")     => o.convertTo[CsvColumnWithDefault[Int]]
                case Some("short")   => o.convertTo[CsvColumnWithDefault[Short]]
                case Some("byte")    => o.convertTo[CsvColumnWithDefault[Byte]]
                case Some("char")    => o.convertTo[CsvColumnWithDefault[Char]]
                case Some("boolean") => o.convertTo[CsvColumnWithDefault[Boolean]]

                case Some("enum") =>
                    enumClassName.map {
                        case name if optional =>
                            val clazz = enumClass getOrElse {
                                throw new DeserializationException(s"class '$name' is not an Enum class.")
                            }
                            o.convertTo(OptionEnumCsvColumn.oeccFormat(clazz))
                        case _ => o.convertTo[EnumCsvColumn]
                    } orElse {
                        Option(o.convertTo[SyntheticEnumCsvColumn])
                    } getOrElse {
                        throw new DeserializationException(s"Couldn't create enum type: $o")
                    }

                case None =>
                    debug(s"No type provided.  Assuming Any.  Given: ${o.compactPrint}")
                    o.convertTo[DefaultCsvColumn]
            }
            spec
        }
    })
}

sealed abstract private[json] class CsvColumnLikeWithDefault[C: RefInfo] extends CsvColumn with Serializable {
    type ColType = C
    val refInfo = RefInfoOps.option(implicitly[RefInfo[C]])
    override def finalizer(sep: String, nullString: String) =
        BasicFinalizer(_.fold(nullString)(_.toString))
}

final case class OptionCsvColumnWithDefault[C: JsonReader: RefInfo](name: String, spec: String, defVal: Option[C] = None)
extends CsvColumnLikeWithDefault[C] {
  /**
    * This makes it the responsibility of the specification to return an option.
    * @return
    */
    override def wrappedSpec = spec
}

final case class CsvColumnWithDefault[C: RefInfo: JsonReader](name: String, spec: String, defVal: Option[C] = None)
extends CsvColumnLikeWithDefault[C]

final case class DefaultCsvColumn(name: String, spec: String) extends CsvColumn {
    type ColType = Any
    def defVal: Option[ColType] = None
    val refInfo = RefInfo[Option[Any]]
    def finalizer(sep: String, nullString: String) = BasicFinalizer(_.fold(nullString)(_.toString))
}

final case class OptionEnumCsvColumn[E <: Enum[E]](name: String, spec: String, enumClass: String, defVal: Option[E] = None)
  extends CsvColumn {

    /**
      * This may throw during the constructor call.  That's the correct time to throw.
      */
    type ColType = E
    private[this] val clazz = Class.forName(enumClass).asInstanceOf[Class[E]]
    def values = clazz.getEnumConstants.map(_.name).toVector
    def refInfo = RefInfoOps.option(RefInfoOps.fromSimpleClass(clazz))
    def finalizer(sep: String, nullString: String) = EncodingBasedFinalizer((e: Encoding) => e.finalizer(sep, nullString, values))
    override def wrappedSpec = spec
}

object OptionEnumCsvColumn {
    def oeccFormat[E <: Enum[E]](clas: Class[_]): RootJsonFormat[OptionEnumCsvColumn[E]] = {
        import DefaultJsonProtocol._
        val s = StringJsonFormat
        val e = optionFormat(Formats.enumFormat(clas.asInstanceOf[Class[E]]))
        jsonFormat(OptionEnumCsvColumn.apply[E], "name", "spec", "enumClass", "defVal")(s, s, s, e)
    }
}

final case class EnumCsvColumn(name: String, spec: String, enumClass: String)
extends CsvColumn {

    /**
     * This may throw during the constructor call.  That's the correct time to throw.
     */
    type ColType = Enum[_]
    private[this] val clazz = Class.forName(enumClass).asInstanceOf[Class[Enum[_]]]
    def values = clazz.getEnumConstants.map(_.name).toVector
    def refInfo = RefInfoOps.option(RefInfoOps.fromSimpleClass(clazz))
    def defVal: Option[Enum[_]] = None
    def finalizer(sep: String, nullString: String) = EncodingBasedFinalizer((e: Encoding) => e.finalizer(sep, nullString, values))
}

sealed abstract private[json] class SyntheticEnumLikeCsvColumn extends CsvColumn {
    type ColType = String
    def refInfo = RefInfo[Option[ColType]]
    def values: Seq[String]
    def finalizer(sep: String, nullString: String) = EncodingBasedFinalizer((e: Encoding) => e.finalizer(sep, nullString, values))
}

final case class SyntheticEnumCsvColumn(name: String, spec: String, values: Seq[String], defVal: Option[String] = None)
  extends SyntheticEnumLikeCsvColumn

final case class OptionSyntheticEnumCsvColumn(name: String, spec: String, values: Seq[String], defVal: Option[String] = None)
  extends SyntheticEnumLikeCsvColumn {
    override def wrappedSpec = spec
}
