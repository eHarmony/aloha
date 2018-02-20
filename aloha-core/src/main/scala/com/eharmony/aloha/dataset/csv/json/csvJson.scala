package com.eharmony.aloha.dataset.csv.json

import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicFinalizer, EncodingBasedFinalizer, Finalizer}
import com.eharmony.aloha.factory.Formats
import com.eharmony.aloha.factory.ri2jf.{RefInfoToJsonFormat, StdRefInfoToJsonFormat}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.util.Logging
import spray.json._

import scala.collection.{immutable => sci}
import scala.util.Try

/**
  * Note that the codomain of functions produced from a CsvColumn, `c`, is `Option[c.ColType]`.
  * This is so that functions with optional variables can be treated the same way as functions
  * with no variables or only required variables.  In order to accomplish this, we need to use
  * `Option` so that if a variable is missing the feature output is `None`.  If all variables
  * are present, it can be a `Some` that wraps the user-designed feature's functionality.
  *
  * We can go even further and allow the feature author to return an `Option` directly and then
  * fall back to the `defVal` prodvided in the specification if the function returns `None`.
  *
  * This is how the feature function compilation works.  It does something like the following:
  *
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

    trait ReaderProducer {
        def apply[A](implicit r: RefInfo[A], f: JsonFormat[A]): JsonReader[_ <: CsvColumn]
    }

    private[this] object CsvColumnSpecWithDefaultFormat extends ReaderProducer {
        override def apply[A](implicit r: RefInfo[A], f: JsonFormat[A]): RootJsonFormat[CsvColumnWithDefault[A]] =
            jsonFormat(CsvColumnWithDefault.apply[A], "name", "spec", "defVal")
    }

    private[this] object OptionCsvColumnSpecWithDefaultFormat extends ReaderProducer {
        override def apply[A](implicit r: RefInfo[A], f: JsonFormat[A]): RootJsonFormat[OptionCsvColumnWithDefault[A]] =
            jsonFormat(OptionCsvColumnWithDefault.apply[A], "name", "spec", "defVal")
    }

    private[this] object SeqCsvColumnSpecWithNoDefaultFormat extends ReaderProducer {
        override def apply[A](implicit r: RefInfo[A], f: JsonFormat[A]): RootJsonFormat[SeqCsvColumnWithNoDefault[A]] =
            jsonFormat(SeqCsvColumnWithNoDefault.apply[A], "name", "spec", "size")
    }

    private[this] object OptionSeqCsvColumnSpecWithNoDefaultFormat extends ReaderProducer {
        override def apply[A](implicit r: RefInfo[A], f: JsonFormat[A]): RootJsonFormat[OptionSeqCsvColumnWithNoDefault[A]] =
            jsonFormat(OptionSeqCsvColumnWithNoDefault.apply[A], "name", "spec", "size")
    }

    private[this] implicit val defaultCsvColumnSpecFormat: RootJsonFormat[DefaultCsvColumn] =
        jsonFormat(DefaultCsvColumn.apply, "name", "spec")

    // Need to for some reason name the fields explicitly here...
    private[this] implicit val enumCsvColumnSpecFormat: RootJsonFormat[EnumCsvColumn] = jsonFormat(EnumCsvColumn.apply, "name", "spec", "enumClass")
    private[this] implicit val syntheticEnumCsvColumnSpecFormat: RootJsonFormat[SyntheticEnumCsvColumn] = jsonFormat(SyntheticEnumCsvColumn.apply, "name", "spec", "values", "defVal")
    private[this] implicit val optionSyntheticEnumCsvColumnSpecFormat: RootJsonFormat[OptionSyntheticEnumCsvColumn] = jsonFormat(OptionSyntheticEnumCsvColumn.apply, "name", "spec", "values", "defVal")

    private val StdTypes =
        Set("string", "double", "float", "long", "int", "short", "byte", "char", "boolean")

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

            val sized = o.getFields("size").nonEmpty

            lazy val enumClassName = o.getFields("enumClass") match {
                case Seq(JsString(c)) => Some(c)
                case _ => None
            }

            lazy val enumClass = enumClassName.flatMap(c =>
                Try {
                    Class.forName(c)
                }.
                  toOption.
                  collect { case e if e.isEnum => e.asInstanceOf[Class[Enum[_]]] }
            )

            implicit val riJf = new StdRefInfoToJsonFormat

            val spec: CsvColumn = (sized, optional) match {
                case (false, false) =>
                    scalar(CsvColumnSpecWithDefaultFormat, reqEnum, fieldType, o, enumClassName, enumClass)
                case (false, true) =>
                    scalar(OptionCsvColumnSpecWithDefaultFormat, optEnum, fieldType, o, enumClassName, enumClass)
                case (true, false) =>
                    vector(SeqCsvColumnSpecWithNoDefaultFormat, fieldType, o)
                case (true, true) =>
                    vector(OptionSeqCsvColumnSpecWithNoDefaultFormat, fieldType, o)
            }

            spec
        }
    })

    private[json] def convertType(
            reader: ReaderProducer,
            aStr: String,
            o: JsObject)(implicit riJf: RefInfoToJsonFormat): Either[String, CsvColumn] = {
        for {
            riUncast <- RefInfo.fromString(aStr.capitalize).right
            ri <- Right(riUncast.asInstanceOf[RefInfo[Any]]).right // cast :-(
            jf <- riJf(ri).toRight(
                s"Couldn't produce JsonFormat from ${RefInfoOps.toString(ri)}"
            ).right
        } yield {
            o.convertTo(reader(ri, jf))
        }
    }

    private[json] def convertTypeOrThrow(
                                          reader: ReaderProducer,
                                          aStr: String,
                                          o: JsObject)(implicit riJf: RefInfoToJsonFormat): CsvColumn = {
        convertType(reader, aStr, o) match {
            case Left(err) => throw new DeserializationException(s"Error: $err occurred for $o")
            case Right(s)  => s
        }
    }

    private[json] def reqEnum(enumClassName: Option[String], enumClass: Option[Class[Enum[_]]], o: JsObject) = {
        Try {
            enumClassName match {
                case Some(_) => o.convertTo[EnumCsvColumn]
                case None    => o.convertTo[SyntheticEnumCsvColumn]
            }
        } getOrElse {
            throw new DeserializationException(s"Couldn't create enum type: $o")
        }
    }

    private[json] def optEnum(enumClassName: Option[String], enumClass: Option[Class[Enum[_]]], o: JsObject) = {
        enumClassName match {
            case Some(name) =>
                val clazz = enumClass getOrElse {
                    throw new DeserializationException(
                        s"class '$name' is not an Enum class for enum type $o."
                    )
                }
                o.convertTo(OptionEnumCsvColumn.oeccFormat(clazz))
            case None =>
                o.convertTo[OptionSyntheticEnumCsvColumn]
        }
    }

    private[json] def scalar(
            rp: ReaderProducer,
            enumFn: (Option[String], Option[Class[Enum[_]]], JsObject) => CsvColumn,
            fieldType: Option[String],
            o: JsObject,
            enumClassName: => Option[String],
            enumClass: => Option[Class[Enum[_]]])(implicit riJf: RefInfoToJsonFormat) = {
        fieldType match {
            case Some(ft) if StdTypes contains ft =>
                convertTypeOrThrow(rp, ft, o)
            case Some("enum") =>
                enumFn(enumClassName, enumClass, o)
            // case Some(d) => Cannot do d
            // case None =>
            case _ =>
                info(s"No type provided.  Assuming Any.  Given: ${o.compactPrint}")
                o.convertTo[DefaultCsvColumn]
        }
    }

    private[json] def vector(
            rp: ReaderProducer,
            fieldType: Option[String],
            o: JsObject)(implicit riJf: RefInfoToJsonFormat) = {

        fieldType match {
            case Some(ft) if StdTypes contains ft =>
                convertTypeOrThrow(rp, ft, o)
            // case Some("enum") =>
            // TODO: Support Enum Case
            // case Some(d) => Cannot do d
            // case None =>
            case _ =>
                // TODO: SUPPORT ENUM
                throw new DeserializationException("Sized fields must of a type in " + (StdTypes - "enum").mkString(", "))
        }
    }
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

sealed abstract private[json] class SeqCsvColumnLikeWithNoDefault[C: JsonReader: RefInfo]
  extends CsvColumn with Serializable {

    override type ColType = Seq[C]
    override def refInfo: RefInfo[Option[ColType]] = RefInfoOps.option[ColType]

    /**
      * Expected size of the sequence output by the feature.
      * @return
      */
    def size: Int

    /**
      * No default.  Default behaviour provided by finalizer.
      * @return
      */
    override def defVal: Option[Seq[C]] = None

    protected def sizeErr: String = s"feature '$name' output size != $size"

    /**
      * If the result is missing, emit a sequence of `nullString` of length `size` and convert to
      * CSV.  If present, emit `size` CSV fields.
      * @param sep column separator
      * @param nullString string used for missing values.
      * @return
      */
    override def finalizer(sep: String, nullString: String): Finalizer[ColType] = {
        BasicFinalizer(_.fold(Iterator.fill(size)(nullString).mkString(sep))(_.mkString(sep)))
    }
}

final case class SeqCsvColumnWithNoDefault[C: JsonReader: RefInfo](name: String, spec: String, size: Int)
  extends SeqCsvColumnLikeWithNoDefault[C] {

    /**
      * '''NOTE''': The wrapped specification includes a requirement that the size of
      * the generated sequence is as specified.  If not, an exception should be thrown.
      * @return
      */
    override def wrappedSpec: String =
        s"Option($spec).map{x => require(x.size == $size, " + s""""$sizeErr"); x}"""
}

final case class OptionSeqCsvColumnWithNoDefault[C: JsonReader: RefInfo](name: String, spec: String, size: Int)
  extends SeqCsvColumnLikeWithNoDefault[C] {
    /**
      * '''NOTE''': The wrapped specification includes a requirement that the size of
      * the generated sequence is as specified.  If not, an exception should be thrown.
      * @return
      */
    override def wrappedSpec: String =
        s"$spec.map{x => require(x.size == $size, " + s""""$sizeErr"); x}"""
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
