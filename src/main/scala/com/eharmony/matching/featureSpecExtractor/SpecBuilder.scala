package com.eharmony.matching.featureSpecExtractor

import java.io.{File, InputStream, Reader}
import java.net.URL
import java.{util => ju}

import com.eharmony.matching.aloha.io.{AlohaReadable, ReadableByString}
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.SpecType.SpecType
import org.apache.commons.{vfs, vfs2}
import spray.json._

import scala.collection.JavaConversions.asScalaBuffer
import scala.util.{Failure, Try}


/**
 * Given a semantics, json specification and an ordered sequence of SpecProducers, ''find the first producer''
 * that applies to creating a Spec from the json specification and use it to instantiate the Spec object.
 * @param semantics a Semantics to be used for creating the Spec.
 * @param producers an ordered sequence of SpecProducers.  These producers form the basis of a
 *                  [[http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern chain of responsibility pattern]].
 *                  Therefore, '''the order is important'''.
 * @param specType
 * @param numBits
 * @tparam A the result type produced by reading from one of the readable formats.
 * @tparam B the implementation of Spec[A] used.
 */
final case class SpecBuilder[A, B <: Spec[A]](
        semantics: CompiledSemantics[A],
        producers: Seq[SpecProducer[A, B]],
        specType: Option[SpecType] = None,
        numBits: Option[Int] = None)
extends AlohaReadable[Try[B]] {

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
    private[this] val specReadable = new ReadableByString[Try[B]] {
        override def fromString(s: String) = fromJson(s.parseJson)
    }

    def fromFile(f: File): Try[B] = specReadable.fromFile(f)
    def fromUrl(u: URL): Try[B] = specReadable.fromUrl(u)
    def fromVfs1(foVfs1: vfs.FileObject): Try[B] = specReadable.fromVfs1(foVfs1)
    def fromVfs2(foVfs2: vfs2.FileObject): Try[B] = specReadable.fromVfs2(foVfs2)
    def fromResource(r: String): Try[B] = specReadable.fromResource(r)
    def fromClasspathResource(r: String): Try[B] = specReadable.fromClasspathResource(r)
    def fromInputStream(is: InputStream): Try[B] = specReadable.fromInputStream(is)
    def fromReader(r: Reader): Try[B] = specReadable.fromReader(r)
    def fromString(s: String): Try[B] = specReadable.fromString(s)

    def fromJson(json: JsValue): Try[B] = {
        // Create a view so that the map function is applied lazily and so find attempts to create Spec
        // instances only until it finds one that works.  If no producers are successful, return a failure.
        val spec = producers.view map { p =>
            for {
                typedData <- p.parse(json)
                spec <- p.getSpec(semantics, typedData)
            } yield spec
        } find {
            _.isSuccess
        } getOrElse { fail(producers) }

        spec
    }

    private[this] def fail(ps: Seq[SpecProducer[A, B]]): Failure[B] =
        Failure { new NoSuchElementException(s"No applicable producer found.  Given ${ps.map(_.name).mkString(", ")}") }
}

object SpecBuilder {

    /**
     * This is a factory to be used from Java.  Since java.util.List is invariant, we provide a different signature
     * that accommodates a Java list created by Arrays.asList(...).
     * @param semantics used to generate the features in the spec.
     * @param producers a Java List of SpecProducers.
     * @tparam A type of semantics
     * @tparam B subtype of Spec objects produced by the SpecBuilder.
     * @return a new Spec builder.
     */
    def apply[A, B <: Spec[A]](
            semantics: CompiledSemantics[A],
            producers: ju.List[_ <: SpecProducer[A, _ <: B]]): SpecBuilder[A, B] =
        SpecBuilder[A, B](semantics, producers.toSeq)
}
