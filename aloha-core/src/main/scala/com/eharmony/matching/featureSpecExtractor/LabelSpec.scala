package com.eharmony.matching.featureSpecExtractor

import com.eharmony.aloha.semantics.func.GenAggFunc

trait LabelSpec[-A] extends Spec[A] {
  def stringLabel: GenAggFunc[A, Option[String]]
}
