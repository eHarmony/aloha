package com.eharmony.aloha.semantics.compiled.plugin.schemabased.accessor

import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema.{FieldDesc, ListField}

sealed trait FieldAccessor {
  val field: FieldDesc
}

sealed trait Dereference extends FieldAccessor {
  val index: Int
}

sealed trait Opt extends FieldAccessor
sealed trait Req extends Opt

case class Optional(field: FieldDesc) extends Opt
case class Required(field: FieldDesc) extends Req
case class DerefReq(field: FieldDesc, index: Int) extends Req with Dereference {
  def toOpt = DerefOpt(field, index)
}
case class DerefOpt(field: FieldDesc, index: Int) extends Opt with Dereference

case class Repeated(field: ListField) extends FieldAccessor
