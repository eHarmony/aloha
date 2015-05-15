package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.semantics.func.GenAggFunc

trait LabelSpec[-A] {
  def label: GenAggFunc[A, String]
}
