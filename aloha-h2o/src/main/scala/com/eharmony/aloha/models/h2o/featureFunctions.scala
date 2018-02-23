package com.eharmony.aloha.models.h2o

import java.{lang => jl}

import com.eharmony.aloha.semantics.func.GenAggFunc
import hex.genmodel.easy.RowData

/**
 * When a RowData observation is provided to a predict method, the value types are extremely restricted.
 * This exception occurs if the value of a RowData element is of the wrong data type.
 * (The only supported value types are `String` and `Double`.)
 *
 * See documentation for `hex.genmodel.easy.exception.PredictUnknownTypeException`.
 */
sealed trait FeatureFunction[-A] {
  def ff: GenAggFunc[A, _]

  /**
    * Fill in the `rowData` object with data extracted by `ff`
    *
    * The returned missing information should be found by:
    *
    * {{{
    * ff.specification -> ff.accessorOutputMissing(a)
    * }}}
    *
    * @param input the input that will be passed to the feature function `ff`.
    * @param name name of the feature.
    * @param rowData the mutable `RowData` object that should be filled
    * @return Missing feature information
    */
  def fillRowData(input: A, name: String, rowData: RowData): Option[(String, Seq[String])]
}

private[h2o] sealed abstract class ScalarFeatureFunction[-A, B](implicit box: B => AnyRef) {
  def ff: GenAggFunc[A, Option[B]]
  def fillRowData(input: A, name: String, rowData: RowData): Option[(String, Seq[String])] = {
    ff(input) match {
      case Some(x) =>
        rowData.put(name, box(x))
        None
      case None =>
        Some(ff.specification -> ff.accessorOutputMissing(input))
    }
  }
}

private[h2o] sealed abstract class VectorFeatureFunction[-A, B](implicit box: B => AnyRef) {
  def size: Int
  def ff: GenAggFunc[A, Option[Seq[B]]]
  def fillRowData(input: A, name: String, rowData: RowData): Option[(String, Seq[String])] = {
    ff(input) match {
      case Some(xs) =>
        xs.zipWithIndex.foreach {
          case(x, i) => rowData.put(s"${name}_$i", box(x))
        }
        None
      case None =>
        Some(ff.specification -> ff.accessorOutputMissing(input))
    }
  }
}

case class DoubleFeatureFunction[-A](ff: GenAggFunc[A, Option[jl.Double]])
   extends ScalarFeatureFunction[A, jl.Double]
      with FeatureFunction[A]

case class StringFeatureFunction[-A](ff: GenAggFunc[A, Option[String]])
   extends ScalarFeatureFunction[A, String]
     with FeatureFunction[A]

case class DoubleSeqFeatureFunction[-A](ff: GenAggFunc[A, Option[Seq[Double]]], size: Int)
   extends VectorFeatureFunction[A, Double]
      with FeatureFunction[A]
