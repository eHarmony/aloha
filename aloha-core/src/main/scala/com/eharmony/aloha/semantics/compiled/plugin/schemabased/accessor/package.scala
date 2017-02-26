package com.eharmony.aloha.semantics.compiled.plugin.schemabased.accessor

import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema.{FieldDesc, ListField}

sealed trait FieldAccessor {
  val field: FieldDesc
}

sealed trait Dereference extends FieldAccessor {
  val index: Int
}

sealed trait Opt extends FieldAccessor
sealed trait Req extends FieldAccessor // Opt

case class Optional(field: FieldDesc) extends Opt
case class Required(field: FieldDesc) extends Req

/**
  * Non-null repeated field (elements may or may not be null) that when
  * dereferenced returns raw type (which may be null)
  */
case class ReqDerefReq(field: ListField, index: Int) extends Req with Dereference {
  def toOpt = ReqDerefOpt(field, index)
}

/**
  * Non-null repeated field (elements may or may not be null) that when
  * dereferenced returns an Option.
  */
case class ReqDerefOpt(field: ListField, index: Int) extends Opt with Dereference

/**
  * Nullable repeated field (elements may or may not be null) that when
  * dereferenced returns an Option.
  */
case class OptDerefOpt(field: ListField, index: Int) extends Opt with Dereference

case class Repeated(field: ListField) extends FieldAccessor
