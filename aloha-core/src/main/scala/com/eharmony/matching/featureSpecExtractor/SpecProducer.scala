package com.eharmony.matching.featureSpecExtractor

import scala.util.Try
import spray.json.JsValue
import com.eharmony.aloha.semantics.compiled.CompiledSemantics

/**
 * SpecProducer is used to create different kinds of [[Spec]] instances.  '''Classes that extend SpecProducer should
 * only have zero-arg constructors.'''  This is because Spec instances should only be parametrized by the JSON
 * specification.  Otherwise, one JSON specification could produce non-equivalent Spec instances in different
 * environments.  It is a design goal for this not to happen.
 *
 * @tparam A type of input passed to the spec.
 * @tparam B implementation of the Spec[A] that is returned by the getSpec function.
 */
trait SpecProducer[A, +B <: Spec[A]] {

    /**
     * Type of parsed JSON object.
     */
    type JsonType

    /**
     * Name of this producer.
     * @return
     */
    def name: String

    /**
     * Attempt to parse the JSON AST to an intermediate representation that is used
     * @param json
     * @return
     */
    def parse(json: JsValue): Try[JsonType]

    /**
     * Attempt to produce a Spec.
     * @param semantics semantics used to make sense of the features in the JsonSpec
     * @param jsonSpec a JSON specification to transform into a Spec.
     * @return
     */
    def getSpec(semantics: CompiledSemantics[A], jsonSpec: JsonType): Try[B]
}
