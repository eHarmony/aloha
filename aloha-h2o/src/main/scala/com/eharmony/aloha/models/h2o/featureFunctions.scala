package com.eharmony.aloha.models.h2o

import java.{lang => jl}

import com.eharmony.aloha.semantics.func.GenAggFunc

/**
 * When a RowData observation is provided to a predict method, the value types are extremely restricted.
 * This exception occurs if the value of a RowData element is of the wrong data type.
 * (The only supported value types are `String` and `Double`.)
 *
 * See documentation for `hex.genmodel.easy.exception.PredictUnknownTypeException`.
 */
sealed trait FeatureFunction[-A]

case class DoubleFeatureFunction[-A](ff: GenAggFunc[A, Option[jl.Double]]) extends FeatureFunction[A]
case class StringFeatureFunction[-A](ff: GenAggFunc[A, Option[String]]) extends FeatureFunction[A]

