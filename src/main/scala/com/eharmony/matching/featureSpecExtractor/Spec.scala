package com.eharmony.matching.featureSpecExtractor


trait Spec[-A] extends (A => (MissingAndErroneousFeatureInfo, CharSequence)) with Serializable
