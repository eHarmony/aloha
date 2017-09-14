package com.eharmony.aloha.dataset

import java.io._
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
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.{Failure, Success, Try}


/**
 * Given a semantics, json specification and an ordered sequence of RowCreatorProducers, ''find the first producer''
 * that applies to creating a Spec from the json specification and use it to instantiate the RowCreator object.
 * @param semantics a Semantics to be used for creating the RowCreator.
 * @param producers an ordered sequence of RowCreatorProducers.  These producers form the basis of a
 *                  [[http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern chain of responsibility pattern]].
 *                  Therefore, '''the order is important'''.
 * @tparam A the result type produced by reading from one of the readable formats.
 * @tparam Impl the implementation of Spec[A] used.
 */
final case class RowCreatorBuilder[A, B, Impl <: RowCreator[A, B]](
        semantics: CompiledSemantics[A],
        producers: List[RowCreatorProducer[A, B, Impl]])
extends AlohaReadable[Try[Impl]]
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
    private[this] val rowCreatorReadable = new ReadableByString[Try[Impl]] {
        override def fromString(s: String) = fromJson(s.parseJson)
    }

    def fromFile(f: File): Try[Impl] = rowCreatorReadable.fromFile(f)
    def fromUrl(u: URL): Try[Impl] = rowCreatorReadable.fromUrl(u)
    def fromVfs1(foVfs1: vfs.FileObject): Try[Impl] = rowCreatorReadable.fromVfs1(foVfs1)
    def fromVfs2(foVfs2: vfs2.FileObject): Try[Impl] = rowCreatorReadable.fromVfs2(foVfs2)
    def fromResource(r: String): Try[Impl] = rowCreatorReadable.fromResource(r)
    def fromClasspathResource(r: String): Try[Impl] = rowCreatorReadable.fromClasspathResource(r)
    def fromInputStream(is: InputStream): Try[Impl] = rowCreatorReadable.fromInputStream(is)
    def fromReader(r: Reader): Try[Impl] = rowCreatorReadable.fromReader(r)
    def fromString(s: String): Try[Impl] = rowCreatorReadable.fromString(s)

    def fromJson(json: JsValue): Try[Impl] = {

        /**
         * Attempt to find a spec that can be instantiated.  Along the way, aggregate the failures so they can be
         * returned and logged.  If a spec can be instantiated, search no more.  Just return it and the failures
         * so far.
         * @param prod spec producers
         * @param failures the aggregated failures encountered so far.
         * @return failures and a possible success.
         */
        @tailrec
        def find(prod: List[RowCreatorProducer[A, B, Impl]], failures: List[Failure[Impl]]): (List[Failure[Impl]], Option[Impl]) = {
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

    private[this] def fail(failures: List[Failure[Impl]]): Failure[Impl] = {
      val s = if (failures.size == 1) "" else "s" // Pluralization
      info {
        val stackMsgs = producers.zip(failures).zipWithIndex.map{ case ((p, e), i) =>
          val throwable = e.failed.get

          val stackTrace = Try {
            val baos = new ByteArrayOutputStream
            throwable.printStackTrace(new PrintStream(baos))
            new String(baos.toByteArray)
          }.getOrElse("").replaceAll("\n", "\n\t\t")

          s"${i+1})\t${p.name}: ${throwable.getMessage}\n\t\t$stackTrace"
        }
        stackMsgs.mkString(s"${failures.size} failure$s occurred while attempting to produce spec:\n\t", "\n\t", "")
      }

      val errs = producers.zip(failures).zipWithIndex.map{ case ((p, e), i) => s"${i+1})\t${p.name}: ${e.failed.get.getMessage}"}
      Failure { new NoSuchElementException(errs.mkString("\n\t", "\n\t", "")) }
    }
}

object RowCreatorBuilder {

    /**
     * This is a factory to be used from Java.  Since java.util.List is invariant, we provide a different signature
     * that accommodates a Java list created by Arrays.asList(...).
     * @param semantics used to generate the features in the spec.
     * @param producers a Java List of SpecProducers.
     * @tparam A type of semantics
     * @tparam Impl subtype of Spec objects produced by the SpecBuilder.
     * @return a new Spec builder.
     */
    def apply[A, B, Impl <: RowCreator[A, B]](
            semantics: CompiledSemantics[A],
            producers: ju.List[_ <: RowCreatorProducer[A, B, _ <: Impl]]): RowCreatorBuilder[A, B, Impl] =
        new RowCreatorBuilder[A, B, Impl](semantics, producers.asScala.toList)
}
