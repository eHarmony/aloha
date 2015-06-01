package com.eharmony.matching.aloha.models.reg

import scala.language.implicitConversions

/** Provides a series of implicit conversions to make the specification of regression models cleaner.
  *
  * Each feature in the Regression model constructs an Iterable[(String, Double)].  Once each feature constructs
  * the iterable, the regression model maps this to a new one prefixed by the feature name.  For instance, in the
  * example that follows, "intercept" would emit a value of type Long which would become a function of type
  * [[com.eharmony.matching.aloha.semantics.func.GenAggFunc]] [A, Long].  This however doesn't match the expected
  * output type of [[com.eharmony.matching.aloha.semantics.func.GenAggFunc]] [A, Iterable[(String, Double)] ].
  * Conversions are provide for {Byte, Short, Int, Long, Float, Double} and the Option equivalents so that can
  * produce specify the translate the JSON key-value pair "intercept": "1234L" to Iterable(("", 1234.0)), which
  * when prefixed will yield Iterable(("intercept", 1234.0))
  *
  * {{{
  * {
  *   "modelType": "Regression",
  *   "modelId": {"id": 0, "name": ""},
  *   "features": {
  *     "intercept": "1234L",
  *     ...
  *   },
  *   ...
  * }
  */
// TODO, think about making this
trait RegressionModelValueToTupleConversions {

    // TODO: Because we are doing implicit conversions, we should definitely log the fact that this happens

    implicit def byteToIterableTuple2EmptyStringDouble(x: Byte): Iterable[(String, Double)] = {
        // TODO: Log
        Iterable(("", x.toDouble))
    }
    implicit def shortToIterableTuple2EmptyStringDouble(x: Short): Iterable[(String, Double)] = {
        // TODO: Log
        Iterable(("", x.toDouble))
    }
    implicit def intToIterableTuple2EmptyStringDouble(x: Int): Iterable[(String, Double)] = {
        // TODO: Log
        Iterable(("", x.toDouble))
    }
    implicit def longToIterableTuple2EmptyStringDouble(x: Long): Iterable[(String, Double)] = {
        // TODO: Log
        Iterable(("", x.toDouble))
    }
    implicit def floatToIterableTuple2EmptyStringDouble(x: Float): Iterable[(String, Double)] = {
        // TODO: Log
        Iterable(("", x.toDouble))
    }
    implicit def doubleToIterableTuple2EmptyStringDouble(x: Double): Iterable[(String, Double)] = {
        // TODO: Log
        Iterable(("", x))
    }

    implicit def byteOptionToIterableTuple2EmptyStringDouble(x: Option[Byte]): Iterable[(String, Double)] = {
        // TODO: Log
        x.map(v => Iterable(("", v.toDouble))) getOrElse Nil
    }
    implicit def shortOptionToIterableTuple2EmptyStringDouble(x: Option[Short]): Iterable[(String, Double)] = {
        // TODO: Log
        x.map(v => Iterable(("", v.toDouble))) getOrElse Nil
    }
    implicit def intOptionToIterableTuple2EmptyStringDouble(x: Option[Int]): Iterable[(String, Double)] = {
        // TODO: Log
        x.map(v => Iterable(("", v.toDouble))) getOrElse Nil
    }
    implicit def longOptionToIterableTuple2EmptyStringDouble(x: Option[Long]): Iterable[(String, Double)] = {
        // TODO: Log
        x.map(v => Iterable(("", v.toDouble))) getOrElse Nil
    }
    implicit def floatOptionToIterableTuple2EmptyStringDouble(x: Option[Float]): Iterable[(String, Double)] = {
        // TODO: Log
        x.map(v => Iterable(("", v.toDouble))) getOrElse Nil
    }
    implicit def doubleOptionToIterableTuple2EmptyStringDouble(x: Option[Double]): Iterable[(String, Double)] = {
        // TODO: Log
        x.map(v => Iterable(("", v))) getOrElse Nil
    }
}

object RegressionModelValueToTupleConversions extends RegressionModelValueToTupleConversions
