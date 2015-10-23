package com.eharmony.aloha.models.h2o

/**
 * H2o likes to return `NaN` as a "''valid''" prediction, especially when covariate data is missing.
 * This is not really acceptable.  So, we actually
 */
sealed trait IllConditioned {
  def errorMsg: String
}
case class IllConditionedScalar(value: Double) extends IllConditioned {
  def errorMsg = s"Ill-conditioned scalar prediction: $value."
}

case class IllConditionedMultinomial(probabilities: Vector[Double], index: Int) extends IllConditioned {
  def errorMsg = s"Ill-conditioned multinomial prediction: index $index of class probabilities: $probabilities"
}
