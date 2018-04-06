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

import scala.annotation.tailrec
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
  *                       output size of the `apply` function.  These are not parallel sequences
  *                       to enforce (at compile time) the requirement that they are the
  *                       same size.
  * @param nullString the string representing null.
  * @tparam A The domain of the row creator.
  */
final case class CsvColumnarRowCreator[-A] private[csv](
    features: FeatureExtractorFunction[A, Seq[String]],
    headersToTypes: Seq[(ColumnName, ColumnType)],
    nullString: String
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

  /**
    * This is the default string representation of null / missing data when
    * a value is not supplied in the JSON specification.
    */
  private[this] val DefaultNullString = ""
  private[this] val Encoding = encoding.Encoding.regular

  private[csv] def innerMostType(r: RefInfo[_]): Either[String, RefInfo[_]] = {
    @tailrec
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
      val nullString = jsonSpec.nullValue.getOrElse(DefaultNullString)
      val encoding = jsonSpec.encoding.getOrElse(Encoding)

      val creator = getCovariates(semantics, jsonSpec, nullString, encoding) map {
        case (cov, headers, types) =>
          val typeStrs = types.map(t => RefInfoOps.toString(t))
          CsvColumnarRowCreator(cov, headers zip typeStrs, nullString)
      }
      creator
    }

    /**
      * Given a `semantics` to interpret feature function specifications, descriptions
      * of the columns in `cj`, a string representation of missing values, and an encoding
      * for categorical data, attempt to create feature functions, a sequence of column names
      * and a sequence of column types.
      *
      * @param semantics used to interpret the feature functions used to produce the columnar data.
      * @param cj descriptions of the features.
      * @param nullString string representation of null / missing values in the output columns.
      * @param encoding the encoding to use for categorical data.
      * @return a Try of a tuple where the first element is feature functions representing the
      *         transformation of data of type `A` to string columns.  The second element is a
      *         sequence of column names, and the third element is a sequence of column types in
      *         the output.
      */
    protected[this] def getCovariates(
        semantics: CompiledSemantics[A],
        cj: CsvJson,
        nullString: String,
        encoding: Encoding
    ): Try[(FeatureExtractorFunction[A, Seq[String]], Vector[String], Vector[RefInfo[_]])] = {


      // Get a new semantics with the imports changed to reflect the imports from the Json Spec
      // Import of ExecutionContext.Implicits.global is necessary.
      val semanticsWithImports = semantics.copy[A](imports = cj.imports)

      /**
        * Compile all of the features in `it`.  Short circuit if there's a problem.
        * @param it feature descriptions
        * @param successes the successful features.
        * @param headers the column headers.  This could be larger than `it.size` because
        *                each feature can produce multiple columns
        * @param types the column types.  This could also be larger than `it.size`.
        * @return a Try of a tuple where the first element is feature functions
        *         representing the transformation of data of type `A` to string columns.
        *         The second element is a sequence of column names, and the third element
        *         is a sequence of column types in the output.
        */
      @tailrec
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

          // Attempt to create the function.
          val f = semanticsWithImports.createFunction[Option[csvCol.ColType]](csvCol.wrappedSpec, Some(csvCol.defVal))(csvCol.refInfo)

          f match {
            case Left(msgs) => Failure { failure(csvCol.name, msgs) }
            case Right(success) =>
              // One csvCol (i.e., feature can produce many columns of output).
              // So we need to get all of the column names for the one feature.
              val headersForCol = encoding.csvHeadersForColumn(csvCol)

              // Type is based on encoding in the case of enums
              // Encoding based column, scalar based column, SeqCsvColumnLikeWithNoDefault
              // This gives a single type.
              val colType = columnType(csvCol, encoding)

              colType match {
                case Left(msg) =>
                  Failure { new IllegalArgumentException(s"For feature ${csvCol.name}, encountered error: $msg.") }
                case Right(tpe) =>
                  // Get the finalizer of type: Option[csvCol.ColType] => Seq[String]
                  //
                  // This is based on the encoding in the case of categorical variables but
                  // not in the case of scalars.
                  //
                  // We use the finalizer to take a feature function output (that may or may
                  // not exist) and turn it into a sequece of Strings.
                  val finalizer = csvCol.columnarFinalizer(nullString) match {
                    case BasicColumnarFinalizer(fnl) => fnl
                    case EncodingBasedColumnarFinalizer(fnl) => fnl(encoding)
                  }

                  // Finalizer converts to Seq[String].  So the final output type is:
                  // GenAggFunc[A, Seq[String]]
                  val strFunc = success.
                    andThenGenAggFunc(_ orElse csvCol.defVal). // Issue #98: Inject defVal on None.
                    andThenGenAggFunc(finalizer)

                  // Notice that for types, we add a many copies of the same type.
                  // These types can be used later to turn the strings back into the desired type.
                  compile(
                    it,
                    (csvCol.name, strFunc) :: successes,
                    headers ++ headersForCol,
                    types ++ Stream.fill(headersForCol.size)(tpe)
                  )
              }

          }
        }
      }

      compile(cj.features.iterator, Nil, Vector.empty, Vector.empty)
    }
  }
}
