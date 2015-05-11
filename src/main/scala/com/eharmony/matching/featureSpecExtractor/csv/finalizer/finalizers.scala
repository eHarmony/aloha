package com.eharmony.matching.featureSpecExtractor.csv.finalizer

import com.eharmony.matching.featureSpecExtractor.csv.encoding.Encoding

sealed trait Finalizer[ColType]
final case class NaryFinalizer[ColType](f: Encoding => Option[ColType] => String) extends Finalizer[ColType]
final case class UnaryFinalizer[ColType](f: Option[ColType] => String) extends Finalizer[ColType]
