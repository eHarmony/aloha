package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.dataset._
import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicColumnarFinalizer, EncodingBasedColumnarFinalizer}
import com.eharmony.aloha.dataset.csv.json.{CsvColumn, CsvJson}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.JsValue

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.{breakOut, immutable => sci}

/**
  * Created by ryan.deak on 2/27/18.
  */
final case class CsvColumnarRowCreator[-A](features: FeatureExtractorFunction[A, Seq[String]], headers: Vector[String])
  extends RowCreator[A, sci.IndexedSeq[String]] {
  override def apply(data: A): (MissingAndErroneousFeatureInfo, sci.IndexedSeq[String]) = {
    val (missing, values) = features(data)
    (missing, values.flatMap(identity)(breakOut))
  }
}

object CsvColumnarRowCreator {
  private[this] val NullString = ""
  private[this] val Encoding = encoding.Encoding.regular

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
        case (cov, headers) => CsvColumnarRowCreator(cov, headers)
      }
      creator
    }

    protected[this] def getCovariates(
        semantics: CompiledSemantics[A],
        cj: CsvJson,
        nullString: String,
        encoding: Encoding
    ): Try[(FeatureExtractorFunction[A, Seq[String]], Vector[String])] = {


      // Get a new semantics with the imports changed to reflect the imports from the Json Spec
      // Import of ExecutionContext.Implicits.global is necessary.
      val semanticsWithImports = semantics.copy[A](imports = cj.imports)


      def compile(it: Iterator[CsvColumn], successes: List[(String, GenAggFunc[A, Seq[String]])], headers: Vector[String]): Try[(FeatureExtractorFunction[A, Seq[String]], Vector[String])] = {
        if (!it.hasNext)
          Success { (StringSeqFeatureExtractorFunction(successes.reverse.toIndexedSeq), headers) }
        else {
          val csvCol = it.next()

          val f = semanticsWithImports.createFunction[Option[csvCol.ColType]](csvCol.wrappedSpec, Some(csvCol.defVal))(csvCol.refInfo)
          f match {
            case Left(msgs) => Failure { failure(csvCol.name, msgs) }
            case Right(success) =>
              val headersForCol = encoding.csvHeadersForColumn(csvCol)

              // Get the finalizer.  This is based on the encoding in the case of categorical variables
              // but not in the case of scalars.
              val finalizer = csvCol.columnarFinalizer(nullString) match {
                case BasicColumnarFinalizer(fnl) => fnl
                case EncodingBasedColumnarFinalizer(fnl) => fnl(encoding)
              }

              val strFunc = success.
                andThenGenAggFunc(_ orElse csvCol.defVal).  // Issue #98: Inject defVal on None.
                andThenGenAggFunc(finalizer)                // Finalizer to convert to string.

              compile(it, (csvCol.name, strFunc) :: successes, headers ++ headersForCol)
          }
        }
      }

      compile(cj.features.iterator, Nil, Vector.empty)
    }
  }
}
