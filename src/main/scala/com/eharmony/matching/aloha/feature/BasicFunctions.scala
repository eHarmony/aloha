package com.eharmony.matching.aloha.feature

import com.eharmony.matching.aloha.models.reg.RegressionModelValueToTupleConversions

/**
  *
  */
object BasicFunctions
    extends DefaultPossessor
    with BasicMath
    with Sos2
    with CustomSos2
    with Indicator
    with Intercept
    with RegressionModelValueToTupleConversions {

    final protected[this] val DefaultForMissingDataInReg = Seq(("=UNK", 1.0))
}
