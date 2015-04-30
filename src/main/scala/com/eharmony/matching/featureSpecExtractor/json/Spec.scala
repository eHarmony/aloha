package com.eharmony.matching.featureSpecExtractor.json

/**
 * Representation of a feature.
 * @param name the feature name.
 * @param spec the feature specification.
 * @param defVal a default ''unordered'' sequence of key-value pairs.
 */
final case class Spec(name: String, spec: String, defVal: Option[Seq[(String, Double)]] = Option(Nil))
