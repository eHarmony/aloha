package com.eharmony.aloha.feature

import com.eharmony.aloha.models.reg.RegressionModelValueToTupleConversions
import com.eharmony.aloha.models.tree.decision.DecisionTreeBoolToOptBoolConversions

/**
 *
 */
object BasicFunctions
  extends DefaultPossessor
  with BasicMath
  with Comparisons
  with Sos2
  with CustomSos2
  with Indicator
  with Intercept
  with TimeConstants
  with SparsityTransforms
  with RegressionModelValueToTupleConversions
  with DecisionTreeBoolToOptBoolConversions
  with SkipGrams
  with MapImplicitRegressionConversion
  with OptionLifting {

  final protected[feature] val DefaultForMissingDataInReg = Seq(("=UNK", 1.0))
}
