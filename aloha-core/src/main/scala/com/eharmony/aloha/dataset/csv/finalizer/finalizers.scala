package com.eharmony.aloha.dataset.csv.finalizer

import com.eharmony.aloha.dataset.csv.encoding.Encoding

sealed trait Finalizer[ColType]
final case class EncodingBasedFinalizer[ColType](f: Encoding => Option[ColType] => String) extends Finalizer[ColType]
final case class BasicFinalizer[ColType](f: Option[ColType] => String) extends Finalizer[ColType]
