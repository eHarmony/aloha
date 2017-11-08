package com.eharmony.aloha.dataset

import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import spray.json.JsValue

import scala.util.Try

/**
  * Created by deaktator on 11/6/17.
  *
  * @tparam A
  * @tparam B
  * @tparam S
  * @tparam Impl
  */
trait StatefulRowCreatorProducer[A, +B, S, +Impl <: StatefulRowCreator[A, B, S]] {

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
    * @param jsonSpec a JSON specification to transform into a StatefulRowCreator.
    * @return
    */
  def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: JsonType): Try[Impl]
}
