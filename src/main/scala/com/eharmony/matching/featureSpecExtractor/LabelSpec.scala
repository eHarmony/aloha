package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.semantics.func.GenAggFunc

trait LabelSpec[-A] extends Spec[A] {
  def label: GenAggFunc[A, String]
}
