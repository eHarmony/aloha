package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.dataset._
import com.eharmony.aloha.dataset.csv.encoding.{Encoding, HotOneEncoding, RegularEncoding, ThermometerEncoding}
import com.eharmony.aloha.dataset.csv.finalizer.{BasicColumnarFinalizer, EncodingBasedColumnarFinalizer}
import com.eharmony.aloha.dataset.csv.json._
import com.eharmony.aloha.dataset.csv.CsvColumnarRowCreator.{ColumnName, ColumnType}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.JsValue

import scala.collection.{breakOut, immutable => sci}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * A `CsvColumnarRowCreator` makes a sequence of `String`-based column values.
  *
  * '''NOTE''': string representations of column types are the simple name for values extending
  * `AnyVal`.  For instance: `"Byte"`, `"Char"`, `"Short"`, `"Int"`, `"Long"`, `"Float"`, `"Double"`
  * and non-`AnyVal`s are the canonical class name.  For instance, Strings are the represented as
  * `"java.lang.String"`.
  *
  * Created by ryan.deak on 2/27/18.
  * @param features a feature extractor function.
  * @param headersToTypes An ordering mapping from column name to type.  The size should be the
  *                       output size of the `apply` function.
  * @tparam A The domain of the row creator.
  */
final case class CsvColumnarRowCreator[-A] private[csv](
    features: FeatureExtractorFunction[A, Seq[String]],
    headersToTypes: Seq[(ColumnName, ColumnType)]
) extends RowCreator[A, sci.IndexedSeq[String]] {

  /**
    * Column names
    * @return
    */
  def headers: Seq[String] = headersToTypes.unzip._1

  /**
    * Returns string representations of column types.  These are the simple name for values
    * extending `AnyVal`.  For instance: `"Byte"`, `"Char"`, `"Short"`, `"Int"`, `"Long"`,
    * `"Float"`, `"Double"` and non-`AnyVal`s are the canonical class name.  For instance,
    * Strings are the represented as `"java.lang.String"`.
    * @return
    */
  def types: Seq[String] = headersToTypes.unzip._2

  override def apply(data: A): (MissingAndErroneousFeatureInfo, sci.IndexedSeq[String]) = {
    val (missing, values) = features(data)
    (missing, values.flatMap(identity)(breakOut))
  }
}

object CsvColumnarRowCreator {
  type ColumnName = String
  type ColumnType = String

  private[this] val NullString = ""
  private[this] val Encoding = encoding.Encoding.regular

  private[csv] def innerMostType(r: RefInfo[_]): Either[String, RefInfo[_]] = {
    def h(root: RefInfo[_], current: RefInfo[_]): Either[String, RefInfo[_]] = {
      RefInfoOps.typeParams(current) match {
        case Nil => Right(current)
        case List(ta) => h(root, ta)
        case d =>
          Left(
            s"Given ${RefInfoOps.toString(current)}, found contained type " +
              RefInfoOps.toString(current) + " with multiple type parameters."
          )
      }
    }
    h(r, r)
  }

  private[csv] def columnType(csvColumn: CsvColumn, encoding: Encoding): Either[String, RefInfo[_]] = {
    csvColumn match {
      case ebc: EncodingBasedColumn =>
        encoding match {
          case RegularEncoding => Right(RefInfo.String)
          case HotOneEncoding | ThermometerEncoding => Right(RefInfo.Int)
        }
      case _ => innerMostType(csvColumn.refInfo)
    }
  }

  final case class Producer[A]()
    extends RowCreatorProducer[A, Seq[String], CsvColumnarRowCreator[A]]
      with RowCreatorProducerName
      with CompilerFailureMessages {

    type JsonType = CsvJson
    def parse(json: JsValue): Try[CsvJson] = Try { json.convertTo[CsvJson] }
    def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: CsvJson): Try[CsvColumnarRowCreator[A]] = {
      val nullString = jsonSpec.nullValue.getOrElse(NullString)
      val encoding = jsonSpec.encoding.getOrElse(Encoding)

      val creator = getCovariates(semantics, jsonSpec, nullString, encoding) map {
        case (cov, headers, types) =>
          val typeStrs = types.map(t => RefInfoOps.toString(t))
          CsvColumnarRowCreator(cov, headers zip typeStrs)
      }
      creator
    }

    protected[this] def getCovariates(
        semantics: CompiledSemantics[A],
        cj: CsvJson,
        nullString: String,
        encoding: Encoding
    ): Try[(FeatureExtractorFunction[A, Seq[String]], Vector[String], Vector[RefInfo[_]])] = {


      // Get a new semantics with the imports changed to reflect the imports from the Json Spec
      // Import of ExecutionContext.Implicits.global is necessary.
      val semanticsWithImports = semantics.copy[A](imports = cj.imports)

      def compile(
          it: Iterator[CsvColumn],
          successes: List[(String, GenAggFunc[A, Seq[String]])],
          headers: Vector[String],
          types: Vector[RefInfo[_]]
      ): Try[(FeatureExtractorFunction[A, Seq[String]], Vector[String], Vector[RefInfo[_]])] = {
        if (!it.hasNext)
          Success { (StringSeqFeatureExtractorFunction(successes.reverse.toIndexedSeq), headers, types) }
        else {
          val csvCol = it.next()

          val f = semanticsWithImports.createFunction[Option[csvCol.ColType]](csvCol.wrappedSpec, Some(csvCol.defVal))(csvCol.refInfo)
          f match {
            case Left(msgs) => Failure { failure(csvCol.name, msgs) }
            case Right(success) =>
              val headersForCol = encoding.csvHeadersForColumn(csvCol)

              // Type is based on encoding in the case of enums
              // Encoding based column, scalar based column, SeqCsvColumnLikeWithNoDefault
              val colType = columnType(csvCol, encoding)

              colType match {
                case Left(msg) =>
                  Failure { new IllegalArgumentException(s"For feature ${csvCol.name}, encountered error: $msg.") }
                case Right(tpe) =>
                  // Get the finalizer.  This is based on the encoding in the case of categorical variables
                  // but not in the case of scalars.
                  val finalizer = csvCol.columnarFinalizer(nullString) match {
                    case BasicColumnarFinalizer(fnl) => fnl
                    case EncodingBasedColumnarFinalizer(fnl) => fnl(encoding)
                  }

                  val strFunc = success.
                    andThenGenAggFunc(_ orElse csvCol.defVal).  // Issue #98: Inject defVal on None.
                    andThenGenAggFunc(finalizer)                // Finalizer to convert to string.

                  compile(it, (csvCol.name, strFunc) :: successes, headers ++ headersForCol, types ++ Stream.fill(headersForCol.size)(tpe))
              }

          }
        }
      }

      compile(cj.features.iterator, Nil, Vector.empty, Vector.empty)
    }
  }
}
