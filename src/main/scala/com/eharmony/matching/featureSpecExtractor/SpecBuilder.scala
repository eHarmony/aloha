package com.eharmony.matching.featureSpecExtractor

import java.{lang => jl, util => ju}

import com.eharmony.matching.aloha.io.{AlohaReadable, ReadableByString}
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.SpecType.SpecType
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
case class SpecBuilder[A, B <: Spec[A]](
        semantics: CompiledSemantics[A],
        producers: Seq[SpecProducer[A, B]],
        specType: Option[SpecType] = None,
        numBits: Option[Int] = None)
extends AlohaReadable[Try[B]]
with ReadableByString[Try[B]] {

    def this(
            semantics: CompiledSemantics[A],
            producers: ju.List[SpecProducer[A, B]],
            specType: Option[SpecType],
            numBits: Option[jl.Integer]) =
        this(semantics, producers.toSeq, specType, numBits.map(_.intValue))

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
        Failure { new NoSuchElementException(s"No applicable producer found.  Given ${ps.map(_.specProducerName).mkString(", ")}") }
}
