package com.eharmony.aloha.dataset

import com.eharmony.aloha.semantics.func.GenAggFunc

trait LabelSpec[-A] extends Spec[A] {
  def stringLabel: GenAggFunc[A, Option[String]]
}
