package com.eharmony.aloha.semantics.compiled.plugin.proto.accessor

import com.google.protobuf.Descriptors.FieldDescriptor

private[proto] sealed trait FieldAccessor {
    val field: FieldDescriptor
}

private[proto] sealed trait Dereference {
    val index: Int
}

private[proto] trait Opt extends FieldAccessor
private[proto] trait Req extends Opt

private[proto] case class Optional(field: FieldDescriptor) extends Opt
private[proto] case class Required(field: FieldDescriptor) extends Req
private[proto] case class DerefReq(field: FieldDescriptor, index: Int) extends Req with Dereference {
    def toOpt = DerefOpt(field, index)
}
private[proto] case class DerefOpt(field: FieldDescriptor, index: Int) extends Opt with Dereference

private[proto] case class Repeated(field: FieldDescriptor) extends FieldAccessor
