package com.eharmony.matching.featureSpecExtractor

/**
 * Representation of a feature.
 * @param featureName the feature name.
 * @param spec the feature specification.
 * @param defaultValue a default ''unordered'' sequence of key-value pairs.
 */
case class FeatureSpec(featureName: String, spec: String, defaultValue: Option[Iterable[(String, Double)]] = None)
