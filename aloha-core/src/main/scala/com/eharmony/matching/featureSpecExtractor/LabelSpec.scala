package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.semantics.func.GenAggFunc

trait LabelSpec[-A] extends Spec[A] {
  def stringLabel: GenAggFunc[A, Option[String]]
}
