package com.eharmony.aloha.score.audit.take2

/**
  * [[TypedAuditor]] provides an extract type `B` that is a subtype of the type `A` passed to the
  * type constructor in `T`.
  * Created by ryan on 12/15/16.
  */
trait TypedAuditor[K, T <: TypeCtor, A, B <: T#TC[A]] extends Auditor[K, T, A]