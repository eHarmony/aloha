package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicFinalizer, EncodingBasedFinalizer}
import com.eharmony.aloha.dataset.csv.json.{CsvColumn, CsvJson}
import com.eharmony.aloha.dataset._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
 * A transformer that takes a value of a specified input type that produces string-based CSV data.
 * @param features a representation of the features used to generate the row ouput.
 * @param headers note that the dimensionality of this vector is equal to the dimensionality of the output vector
 *                produced by features rather than the number of features used to produce the vector.  This is because
 *                categorical variables can be expanded in different ways based on the
 *                [[com.eharmony.aloha.dataset.csv.encoding.Encoding]] used.
 * @param separator the field separator
 * @tparam A the input type that is transformed into CSV output.
 */
final case class CsvRowCreator[-A](features: FeatureExtractorFunction[A, String], headers: Vector[String], separator: String = ",")
    extends RowCreator[A] {
    def apply(data: A) = {
        val (missing, values) = features(data)
        (missing, values.mkString(separator))
    }

    /**
     * @return A string containing all the headers associated with the CSV rows created by this [[RowCreator]].
     */
    def headerString = headers.mkString(separator)
}

final object CsvRowCreator {

    /**
     * Tab is the default separator to be consistent with *nix tools like cut, paste.
     */
    private[this] val Separator = "\t"
    private[this] val NullString = ""
    private[this] val Encoding = encoding.Encoding.regular

    final case class Producer[A]()
        extends RowCreatorProducer[A, CsvRowCreator[A]]
        with RowCreatorProducerName
        with CompilerFailureMessages {

        type JsonType = CsvJson
        def parse(json: JsValue): Try[CsvJson] = Try { json.convertTo[CsvJson] }
        def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: CsvJson): Try[CsvRowCreator[A]] = {
            val nullString = jsonSpec.nullValue.getOrElse(NullString)
            val separator = jsonSpec.separator.getOrElse(Separator)
            val encoding = jsonSpec.encoding.getOrElse(Encoding)

            val creator = getCovariates(semantics, jsonSpec, nullString, separator, encoding) map { case (cov, headers) => CsvRowCreator(cov, headers, separator) }
            creator
        }

        protected[this] def getCovariates(
                semantics: CompiledSemantics[A],
                cj: CsvJson,
                nullString: String,
                separator: String,
                encoding: Encoding): Try[(FeatureExtractorFunction[A, String], Vector[String])] = {


            // Get a new semantics with the imports changed to reflect the imports from the Json Spec
            // Import of ExecutionContext.Implicits.global is necessary.
            val semanticsWithImports = semantics.copy[A](imports = cj.imports)


            def compile(it: Iterator[CsvColumn], successes: List[(String, GenAggFunc[A, String])], headers: Vector[String]): Try[(FeatureExtractorFunction[A, String], Vector[String])] = {
                if (!it.hasNext)
                    Success { (StringFeatureExtractorFunction(successes.reverse.toIndexedSeq), headers) }
                else {
                    val csvCol = it.next()

                    val f = semanticsWithImports.createFunction[Option[csvCol.ColType]](csvCol.wrappedSpec, Some(csvCol.defVal))(csvCol.refInfo)
                    f match {
                        case Left(msgs) => Failure { failure(csvCol.name, msgs) }
                        case Right(success) =>
                            val headersForCol = encoding.csvHeadersForColumn(csvCol)

                            // Get the finalizer.  This is based on the encoding in the case of categorical variables
                            // but not in the case of scalars.
                            val finalizer = csvCol.finalizer(separator, nullString) match {
                                case BasicFinalizer(fnl) => fnl
                                case EncodingBasedFinalizer(fnl) => fnl(encoding)
                            }

                            val strFunc = success.
                              andThenGenAggFunc(_ orElse csvCol.defVal).  // Issue #98: Inject defVal on None.
                              andThenGenAggFunc(finalizer)                 // Finalizer to convert to string.

                            compile(it, (csvCol.name, strFunc) :: successes, headers ++ headersForCol)
                    }
                }
            }

            compile(cj.features.iterator, Nil, Vector.empty)
        }
    }
}
