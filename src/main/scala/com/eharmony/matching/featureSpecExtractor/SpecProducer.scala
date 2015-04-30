package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.featureSpecExtractor.json.JsonSpec

import scala.util.Try

/**
 * SpecProducer is used to create different kinds of [[Spec]] instances.
 */
trait SpecProducer {

    /**
     * Should be getClass.getSimpleName for uniformity.
     * @return
     */
    def specProducerName: String

    /**
     * Whether this spec producer can be applied to the given JsonSpec.
     * @param jsonSpec a JSON specification.
     * @return
     */
    def appliesTo(jsonSpec: JsonSpec): Boolean

    /**
     * Attempt to produce a Spec.
     * @param semantics semantics used to make sense of the features in the JsonSpec
     * @param jsonSpec a JSON specification to transform into a Spec.
     * @tparam A type of input passed to the spec.
     * @return
     */
    def getSpec[A](semantics: CompiledSemantics[A], jsonSpec: JsonSpec): Try[Spec[A]]
}
