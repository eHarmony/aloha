package com.eharmony.matching.featureSpecExtractor


trait Spec[A] extends Serializable {
    def toInput(data: A): (MissingAndErroneousFeatureInfo, String)
    def toInput(data: A,includeZeroValues: Boolean): (MissingAndErroneousFeatureInfo, String)
}
