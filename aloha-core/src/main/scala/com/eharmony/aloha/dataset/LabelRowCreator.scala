package com.eharmony.aloha.dataset

import com.eharmony.aloha.semantics.func.GenAggFunc

trait LabelRowCreator[-A, +B] extends RowCreator[A, B] {
  def stringLabel: GenAggFunc[A, Option[String]]
}
