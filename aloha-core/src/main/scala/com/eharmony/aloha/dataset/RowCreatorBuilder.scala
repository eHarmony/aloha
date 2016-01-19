package com.eharmony.aloha.dataset

import java.io.{File, InputStream, Reader}
import java.net.URL
import java.{util => ju}

import com.eharmony.aloha.AlohaException
import com.eharmony.aloha.dataset.json.validation.Validation
import com.eharmony.aloha.io.{AlohaReadable, ReadableByString}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.util.Logging
import org.apache.commons.{vfs, vfs2}
import spray.json._

import scala.annotation.tailrec
import scala.collection.JavaConversions.asScalaBuffer
import scala.util.{Failure, Success, Try}


/**
 * Given a semantics, json specification and an ordered sequence of RowCreatorProducers, ''find the first producer''
 * that applies to creating a Spec from the json specification and use it to instantiate the RowCreator object.
 * @param semantics a Semantics to be used for creating the RowCreator.
 * @param producers an ordered sequence of RowCreatorProducers.  These producers form the basis of a
 *                  [[http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern chain of responsibility pattern]].
 *                  Therefore, '''the order is important'''.
 * @tparam A the result type produced by reading from one of the readable formats.
 * @tparam B the implementation of Spec[A] used.
 */
final case class RowCreatorBuilder[A, B <: RowCreator[A]](
        semantics: CompiledSemantics[A],
        producers: List[RowCreatorProducer[A, B]])
extends AlohaReadable[Try[B]]
   with Logging {

    /**
     * Private inner reader delegates to the fromJson method.  All factory (''from*'') functions in the
     * class delegate to the implementations in the specReadable.  This is because there is a a compiler bug
     * that causes the return types in the generated method signatures to be Object rather than
     * ''Try[B]'' when not doing this.  So, we explicitly create those methods.  For more information, see
     * scala compiler bug:
     *
     *   https://issues.scala-lang.org/browse/SI-8905
     */
    // TODO: Determine if Serializable is necessary to serialize SpecBuilder.
    private[this] val rowCreatorReadable = new ReadableByString[Try[B]] {
        override def fromString(s: String) = fromJson(s.parseJson)
    }

    def fromFile(f: File): Try[B] = rowCreatorReadable.fromFile(f)
    def fromUrl(u: URL): Try[B] = rowCreatorReadable.fromUrl(u)
    def fromVfs1(foVfs1: vfs.FileObject): Try[B] = rowCreatorReadable.fromVfs1(foVfs1)
    def fromVfs2(foVfs2: vfs2.FileObject): Try[B] = rowCreatorReadable.fromVfs2(foVfs2)
    def fromResource(r: String): Try[B] = rowCreatorReadable.fromResource(r)
    def fromClasspathResource(r: String): Try[B] = rowCreatorReadable.fromClasspathResource(r)
    def fromInputStream(is: InputStream): Try[B] = rowCreatorReadable.fromInputStream(is)
    def fromReader(r: Reader): Try[B] = rowCreatorReadable.fromReader(r)
    def fromString(s: String): Try[B] = rowCreatorReadable.fromString(s)

    def fromJson(json: JsValue): Try[B] = {

        /**
         * Attempt to find a spec that can be instantiated.  Along the way, aggregate the failures so they can be
         * returned and logged.  If a spec can be instantiated, search no more.  Just return it and the failures
         * so far.
         * @param prod spec producers
         * @param failures the aggregated failures encountered so far.
         * @return failures and a possible success.
         */
        @tailrec
        def find(prod: List[RowCreatorProducer[A, B]], failures: List[Failure[B]]): (List[Failure[B]], Option[B]) = {
            prod match {
                case Nil => (failures.reverse, None)
                case p :: tail =>
                    val spec = for {
                        typedData <- p.parse(json)                           // Get the intermediate repr (IR).
                        _ <- typedData match {                               // Validate if possible.
                            case v: Validation => v.validate().fold(Try(())){f => Failure(new AlohaException(f)) }
                            case _ => Try(())
                        }
                        spec <- p.getRowCreator(semantics, typedData)              // Attempt to produce the spec from IR.
                    } yield spec

                    spec match {
                        case Success(s) => (failures.reverse, Option(s))     // Done.
                        case f@Failure(e) => find(tail, f :: failures)       // Keep searching.  Recurse.
                    }
            }
        }

        val (failures, possibleSuccess) = find(producers, Nil)
        possibleSuccess map {s => Try(s)} getOrElse fail(failures)
    }

    private[this] def fail(failures: List[Failure[B]]): Failure[B] = {
      val s = if (failures.size == 1) "" else "s" // Pluralization
      val msgs = producers.zip(failures).zipWithIndex.map{ case ((p, e), i) => s"$i)\t${p.name}: ${e.failed.get.getMessage}\n\t\t${e.failed.get.getStackTraceString.replaceAll("\n", "\n\t\t")}"}
      val msg = msgs.mkString(s"${failures.size} failure$s occurred while attempting to produce spec:\n\t", "\n\t", "")
      info(msg)
      val finalMsg = s"No applicable producer found.  Given ${producers.map(_.name).mkString(", ")}.\nError Msgs: $msg"
      Failure { new NoSuchElementException(finalMsg) }
    }
}

object RowCreatorBuilder {

    /**
     * This is a factory to be used from Java.  Since java.util.List is invariant, we provide a different signature
     * that accommodates a Java list created by Arrays.asList(...).
     * @param semantics used to generate the features in the spec.
     * @param producers a Java List of SpecProducers.
     * @tparam A type of semantics
     * @tparam B subtype of Spec objects produced by the SpecBuilder.
     * @return a new Spec builder.
     */
    def apply[A, B <: RowCreator[A]](
            semantics: CompiledSemantics[A],
            producers: ju.List[_ <: RowCreatorProducer[A, _ <: B]]): RowCreatorBuilder[A, B] =
        RowCreatorBuilder[A, B](semantics, producers.toList)
}
