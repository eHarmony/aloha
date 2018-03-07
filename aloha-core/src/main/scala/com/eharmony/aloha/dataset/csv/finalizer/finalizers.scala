package com.eharmony.aloha.dataset.csv.finalizer

import com.eharmony.aloha.dataset.csv.encoding.Encoding

sealed trait Finalizer[ColType]
final case class EncodingBasedFinalizer[ColType](f: Encoding => Option[ColType] => String) extends Finalizer[ColType]
final case class BasicFinalizer[ColType](f: Option[ColType] => String) extends Finalizer[ColType]

sealed trait ColumnarFinalizer[ColType]
final case class EncodingBasedColumnarFinalizer[ColType](f: Encoding => Option[ColType] => Seq[String]) extends ColumnarFinalizer[ColType]
final case class BasicColumnarFinalizer[ColType](f: Option[ColType] => Seq[String]) extends ColumnarFinalizer[ColType]
