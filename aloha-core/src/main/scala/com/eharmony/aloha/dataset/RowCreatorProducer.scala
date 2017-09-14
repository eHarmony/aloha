package com.eharmony.aloha.dataset

import scala.util.Try
import spray.json.JsValue
import com.eharmony.aloha.semantics.compiled.CompiledSemantics

/**
  * [[RowCreatorProducer]] is used to create different kinds of [[RowCreator]] instances.
  * '''Classes that extend [[RowCreatorProducer]] should only have zero-arg constructors.'''
  * This is because Spec instances should only be parametrized by the JSON
  * specification.  Otherwise, one JSON specification could produce non-equivalent
  * Spec instances in different environments.
  *
  * '''It is a design goal for this not to happen.'''
  *
  * @tparam A type of input passed to the [[RowCreator]].
  * @tparam B type of output returned from the [[RowCreator]].
  * @tparam Impl implementation of the Spec[A] that is returned by the `getRowCreator` function.
  */
trait RowCreatorProducer[A, +B , +Impl <: RowCreator[A, B]] {

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
     * @param jsonSpec a JSON specification to transform into a RowCreator.
     * @return
     */
    def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: JsonType): Try[Impl]
}
