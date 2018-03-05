package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.dataset._
import com.eharmony.aloha.dataset.csv.encoding.Encoding
import com.eharmony.aloha.dataset.csv.finalizer.{BasicAnyColumnarFinalizer, EncodingBasedAnyColumnarFinalizer}
import com.eharmony.aloha.dataset.csv.json.{CsvColumn, CsvJson}
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.JsValue

import scala.collection.{breakOut, immutable => sci}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ryan.deak on 3/5/18.
  */
final case class CsvAnyColumnarRowCreator[-A] private[csv](
    features: FeatureExtractorFunction[A, Seq[Option[Any]]],
    headersAndTypes: Seq[(String, RefInfo[Option[_]])]
) extends RowCreator[A, sci.IndexedSeq[Option[Any]]] {

  def headers: Seq[String] = headersAndTypes.unzip._1
  def types: Seq[RefInfo[Option[_]]] = headersAndTypes.unzip._2

  override def apply(data: A): (MissingAndErroneousFeatureInfo, sci.IndexedSeq[Option[Any]]) = {
    val (missing, values) = features(data)
    (missing, values.flatMap(identity)(breakOut))
  }
}

object CsvAnyColumnarRowCreator {
  private[this] val Encoding = encoding.Encoding.regular

  final case class Producer[A]()
    extends RowCreatorProducer[A, Seq[Option[Any]], CsvAnyColumnarRowCreator[A]]
      with RowCreatorProducerName
      with CompilerFailureMessages {

    type JsonType = CsvJson
    def parse(json: JsValue): Try[CsvJson] = Try { json.convertTo[CsvJson] }
    def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: CsvJson): Try[CsvAnyColumnarRowCreator[A]] = {
      val encoding = jsonSpec.encoding.getOrElse(Encoding)

      val creator = getCovariates(semantics, jsonSpec, encoding) map {
        case (cov, headersAndTypes) => CsvAnyColumnarRowCreator(cov, headersAndTypes)
      }
      creator
    }

    protected[this] def getCovariates(
        semantics: CompiledSemantics[A],
        cj: CsvJson,
        encoding: Encoding
    ): Try[(FeatureExtractorFunction[A, Seq[Option[Any]]], Vector[(String, RefInfo[Option[_]])])] = {

      // Get a new semantics with the imports changed to reflect the imports from the Json Spec
      // Import of ExecutionContext.Implicits.global is necessary.
      val semanticsWithImports = semantics.copy[A](imports = cj.imports)

      def compile(
          it: Iterator[CsvColumn],
          successes: List[(String, GenAggFunc[A, Seq[Option[Any]]])],
          headers: Vector[(String, RefInfo[Option[_]])]
      ): Try[(FeatureExtractorFunction[A, Seq[Option[Any]]], Vector[(String, RefInfo[Option[_]])])] = {

        if (!it.hasNext)
          Success { (OptionAnySeqFeatureExtractorFunction(successes.reverse.toIndexedSeq), headers) }
        else {
          val csvCol = it.next()

          val tpe = columnType(csvCol.refInfo)
          val outType = RefInfoOps.wrap(tpe).in[Seq]

//          val f = semanticsWithImports.createFunction[Option[csvCol.ColType]](csvCol.wrappedSpec, Some(csvCol.defVal))(csvCol.refInfo)
          val f = semanticsWithImports.createFunction[Seq[Option[_]]](csvCol.wrappedSpec, None)(outType)
          f match {
            case Left(msgs) => Failure { failure(csvCol.name, msgs) }
            case Right(success: GenAggFunc[A, Seq[Option[_]]]) =>
              val headersForCol = encoding.csvHeadersForColumn(csvCol)
              val headerAndTypesForCol = headersForCol.zip(Stream.continually(tpe))

              // Get the finalizer.  This is based on the encoding in the case of categorical variables
              // but not in the case of scalars.
              val finalizer = csvCol.anyColumnarFinalizer match {
                case BasicAnyColumnarFinalizer(fnl) => fnl
                case EncodingBasedAnyColumnarFinalizer(fnl) => fnl(encoding)
              }

              val strFunc = success.
                andThenGenAggFunc(_ orElse csvCol.defVal).  // Issue #98: Inject defVal on None.
                andThenGenAggFunc(finalizer)                // Finalizer to convert to string.

              compile(it, (csvCol.name, strFunc) :: successes, headers ++ headerAndTypesForCol)
          }
        }
      }

      compile(cj.features.iterator, Nil, Vector.empty)
    }
  }

  private[csv] def columnType(r: RefInfo[_]): RefInfo[Option[_]] = {
    def h(top: RefInfo[_], ri: RefInfo[_]): RefInfo[Option[_]] = {
      RefInfoOps.typeParams(ri) match {
        case Nil =>
          // Cast gets around compiler correctly inferring that these are not the same types.
          RefInfoOps.option(ri)
            .asInstanceOf[RefInfo[Option[_]]]
        case List(ta) => h(top, ta)
        case d => throw new IllegalStateException(
          s"The type ${RefInfoOps.toString(top)} should only have nested types of one parameter.  " +
          s"Found nested type ${RefInfoOps.toString(ri)} with ${d.size} type parameters."
        )
      }
    }
    h(r, r)
  }
}
