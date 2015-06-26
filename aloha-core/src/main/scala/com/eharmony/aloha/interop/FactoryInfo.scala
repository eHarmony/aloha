package com.eharmony.aloha.interop

import spray.json.JsonReader

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.conversions.ScoreConverter

/** These are the variables that need to be passed to the ModelFactory to create the models.  This is typically done
  * via an implicit argument list but for interoperability with Java, this is problematic.  The strategy instead is to
  * pass an instance of FactoryInfo to either create a factory or to get back a model from a ModelFactory.  This second
  * use case seems less likely because Java doesn't seem to have the same understanding of the type information
  * related to the higher kinds that are passed to constructor of ModelFactory.  So, we provide a way around this by
  * simplifying the interface that will typically be called from the Java world.
  *
  * The way to do this is to create a NoImplModelFactory given a list of
  * @tparam A input type of models to be created
  * @tparam B output type of models to be created
  */
trait FactoryInfo[A, B] {
    val inRefInfo: RefInfo[A]
    val outRefInfo: RefInfo[B]
    val jsonReader: JsonReader[B]
    val scoreConverter: ScoreConverter[B]
}
