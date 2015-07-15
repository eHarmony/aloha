package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicFinalizer, EncodingBasedFinalizer}
import com.eharmony.aloha.dataset.csv.json.{CsvColumn, CsvJson}
import com.eharmony.aloha.dataset.{CompilerFailureMessages, FeatureExtractorFunction, RowCreator, RowCreatorProducer, StringFeatureExtractorFunction}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

final case class CsvRowCreator[-A](features: FeatureExtractorFunction[A, String], separator: String = ",")
    extends RowCreator[A] {
    def apply(data: A) = {
        val (missing, values) = features(data)
        (missing, values.mkString(separator))
    }
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
        with CompilerFailureMessages {

        type JsonType = CsvJson
        def name = getClass.getSimpleName
        def parse(json: JsValue): Try[CsvJson] = Try { json.convertTo[CsvJson] }
        def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: CsvJson): Try[CsvRowCreator[A]] = {
            val nullString = jsonSpec.nullValue.getOrElse(NullString)
            val separator = jsonSpec.separator.getOrElse(Separator)
            val encoding = jsonSpec.encoding.getOrElse(Encoding)

            val spec = getCovariates(semantics, jsonSpec, nullString, separator, encoding) map { cov => CsvRowCreator(cov, separator) }
            spec
        }

        protected[this] def getCovariates(
                semantics: CompiledSemantics[A],
                cj: CsvJson,
                nullString: String,
                separator: String,
                encoding: Encoding): Try[FeatureExtractorFunction[A, String]] = {

            // Get a new semantics with the imports changed to reflect the imports from the Json Spec
            // Import of ExecutionContext.Implicits.global is necessary.
            val semanticsWithImports = semantics.copy[A](imports = cj.imports)


            def compile(it: Iterator[CsvColumn], successes: List[(String, GenAggFunc[A, String])]): Try[FeatureExtractorFunction[A, String]] = {
                if (!it.hasNext)
                    Success { StringFeatureExtractorFunction(successes.reverse.toIndexedSeq) }
                else {
                    val spec = it.next()

                    val f = semanticsWithImports.createFunction[Option[spec.ColType]](spec.wrappedSpec, Some(spec.defVal))(spec.refInfo)
                    f match {
                        case Left(msgs) => Failure { failure(spec.name, msgs) }
                        case Right(success) =>

                            // Get the finalizer.  This is based on the encoding in the case of categorical variables
                            // but not in the case of scalars.
                            val finalizer = spec.finalizer(separator, nullString) match {
                                case BasicFinalizer(fnl) => fnl
                                case EncodingBasedFinalizer(fnl) => fnl(encoding)
                            }

                            val strFunc = success.andThenGenAggFunc(finalizer)
                            compile(it, (spec.name, strFunc) :: successes)
                    }
                }
            }

            compile(cj.features.iterator, Nil)
        }
    }
}
