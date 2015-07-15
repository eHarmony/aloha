package com.eharmony.aloha.dataset

import com.eharmony.aloha.semantics.func.GenAggFunc

trait LabelRowCreator[-A] extends RowCreator[A] {
  def stringLabel: GenAggFunc[A, Option[String]]
}
