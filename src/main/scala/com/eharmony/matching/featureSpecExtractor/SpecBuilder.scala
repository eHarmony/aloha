package com.eharmony.matching.featureSpecExtractor

import java.{lang => jl, util => ju}

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor
import com.eharmony.matching.featureSpecExtractor.SpecType.SpecType
import com.eharmony.matching.featureSpecExtractor.json.JsonSpec
import com.google.common.base.Optional

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.{immutable => sci}
import scala.util.{Failure, Try}

object SpecBuilder {
    private[this] val DefaultHashBits = 18

    def build[A](
            semantics: CompiledSemantics[A],
            json: String,
            specType: Optional[SpecType],
            numBits: Optional[jl.Integer],
            producers: ju.List[SpecProducer]): Try[Spec[A]] = {
        val s = JsonSpec.fromString(json)
        build(semantics, s, specType, numBits, producers)
    }

    def build[A](
            semantics: CompiledSemantics[A],
            jsonSpec: JsonSpec,
            specType: Optional[SpecType],
            numBits: Optional[jl.Integer],
            producers: ju.List[SpecProducer]): Try[Spec[A]] =
        build(semantics, jsonSpec, Option(specType.orNull()), Option(numBits.orNull()).map(_.intValue()), producers.toSeq)

    def build[A](
            semantics: CompiledSemantics[A],
            json: String,
            specType: Option[SpecType],
            numBits: Option[Int],
            producers: Seq[SpecProducer]): Try[Spec[A]] = {
        val s = JsonSpec.fromString(json)
        build(semantics, s, specType, numBits, producers)
    }

    /**
     * Given a semantics and specType and an ordered sequence of SpecProducers, ''find the first producer'' that
     * applies and use it to instantiate the Spec object.
     * @param semantics a Semantics to be used for creating the Spec.
     * @param jsonSpec a JSON specification.
     * @param specType a hint on the type of spec to produce.  If the specType field is found in jsonSpec,
     *                 this parameter is ignored.  If the field is not present, then this will be used to
     *                 populate the field in jsonSpec.
     * @param numBits the number of bits to use in feature hashing if applicable.  If the numBits field is found
     *                in jsonSpec, this parameter is ignored.  If the field is not present, then this will be
     *                used to populate the field in jsonSpec.  If neither are present, DefaultHashBits will be inserted
     *                into the JsonSpec.
     * @param producers an ordered sequence of SpecProducers.  These producers form the basis of a
     *                  [[http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern chain of responsibility pattern]].
     *                  Therefore, '''the order is important'''.
     * @tparam A type of values the produced Spec will accept.
     * @return
     */
    def build[A](
            semantics: CompiledSemantics[A],
            jsonSpec: JsonSpec,
            specType: Option[SpecType],
            numBits: Option[Int],
            producers: Seq[SpecProducer]): Try[Spec[A]] = {

        val copySpec = jsonSpec.specType.isEmpty && specType.nonEmpty
        val copyBits = jsonSpec.numBits.isEmpty && numBits.nonEmpty
        val defaultBits = jsonSpec.numBits.isEmpty && numBits.isEmpty

        val adaptedSpec =
            if (!(copySpec || copyBits || defaultBits)) jsonSpec
            else {
                val t = jsonSpec.specType.orElse(specType.map(_.toString))
                val b = jsonSpec.numBits.orElse(numBits).orElse(Option(DefaultHashBits))
                jsonSpec.copy(specType = t, numBits = b)
            }

        val spec: Try[Spec[A]] = getSpec(semantics, adaptedSpec, producers)
        spec
    }

    /**
     * Given a semantics, json specification and an ordered sequence of SpecProducers, ''find the first producer''
     * that applies to creating a Spec from the json specification and use it to instantiate the Spec object.
     * @param semantics a Semantics to be used for creating the Spec.
     * @param jsonSpec a JSON specification.
     * @param producers an ordered sequence of SpecProducers.  These producers form the basis of a
     *                  [[http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern chain of responsibility pattern]].
     *                  Therefore, '''the order is important'''.
     * @tparam A the type of semantics.
     * @return
     */
    final protected[this] def getSpec[A](semantics: CompiledSemantics[A], jsonSpec: JsonSpec, producers: Seq[SpecProducer]): Try[featureSpecExtractor.Spec[A]] = {
        val producerCandidate = producers.find(p => p.appliesTo(jsonSpec))

        val spec = for {
            p <- producerCandidate map { pc => Try(pc) } getOrElse fail(producers)
            s <- p.getSpec(semantics, jsonSpec)
        } yield s

        spec
    }

    private[this] final def fail[A](ps: Seq[SpecProducer]): Failure[SpecProducer] =
        Failure{ new NoSuchElementException(s"No applicable producer found.  Given ${ps.map(_.specProducerName).mkString(", ")}")}
}
