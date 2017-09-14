package com.eharmony.aloha.dataset

import scala.util.Try
import spray.json.JsValue
import com.eharmony.aloha.semantics.compiled.CompiledSemantics

/**
  * [[RowCreatorProducer]] is used to create different kinds of [[RowCreator]] instances.
  *
  * '''Classes that extend [[RowCreatorProducer]] should (''try to'') have only
  * zero-argument constructors.'''
  *
  * This is because [[RowCreator]] instances should ideally only be parametrized by
  * the JSON specification.  Otherwise, one JSON specification could produce
  * non-equivalent [[RowCreator]] instances in different environments.
  *
  * '''This statelessness is a design goal and should only be broken with good reason.'''
  *
  * One of the reasons this rule ''will likely be broken'' is that things
  * like context bounds on a type parameter to a [[RowCreatorProducer]] become
  * constructor arguments.  So if a [[RowCreatorProducer]] is parametrized by a
  * type that requires a type class to decode the JSON representation, this rule would be
  * broken.
  *
  * Another example might be in training multi-label models.  Whereas in binary classifiers
  * the labels values are known automatically (because they are isomorphic to the set
  * `{true, false}`), the label set isn't known ''a priori'' (because each problem codomain
  * might be different).  Therefore, we might ask for the set of labels to expect.
  *
  * '''NOTE''': `com.eharmony.aloha.dataset.RowCreatorProducerTest` will be used to control
  * which [[RowCreatorProducer]]s can accept parameters.
  *
  * @tparam A type of input passed to the [[RowCreator]].
  * @tparam B type of output returned from the [[RowCreator]].
  * @tparam Impl implementation of the [[RowCreator]] that is returned by the
  *              `getRowCreator` function.
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
