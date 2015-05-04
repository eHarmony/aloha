package com.eharmony.matching.featureSpecExtractor

import java.{lang => jl, util => ju}

import com.eharmony.matching.aloha.io.{AlohaReadable, ReadableByString}
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.SpecType.SpecType
import spray.json._

import scala.annotation.varargs
import scala.collection.JavaConversions.asScalaBuffer
import scala.util.{Failure, Try}


/**
 * Given a semantics, json specification and an ordered sequence of SpecProducers, ''find the first producer''
 * that applies to creating a Spec from the json specification and use it to instantiate the Spec object.
 * @param semantics a Semantics to be used for creating the Spec.
 * @param specType
 * @param numBits
 * @param producers an ordered sequence of SpecProducers.  These producers form the basis of a
 *                  [[http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern chain of responsibility pattern]].
 *                  Therefore, '''the order is important'''.
 * @tparam A the result type produced by reading from one of the readable formats.
 * @tparam B the implementation of Spec[A] used.
 */
case class SpecBuilder[A, B <: Spec[A]](
        semantics: CompiledSemantics[A],
        specType: Option[SpecType] = None,
        numBits: Option[Int] = None,
        producers: Seq[SpecProducer[A, B]])
extends AlohaReadable[Try[B]]
with ReadableByString[Try[B]] {


    def fromString(s: String) = fromJson(s.parseJson)

    def fromJson(json: JsValue): Try[B] = {
        // create a view so that the map function is applied lazily and then find the first success.
        // If no producers are successful, return a failure.
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
    @varargs
    def apply[A, B <: Spec[A]](
            semantics: CompiledSemantics[A],
            producers: ju.List[_ <: SpecProducer[A, _ <: B]]): SpecBuilder[A, B] =
        SpecBuilder(semantics, None, None, producers.toSeq)
}
