package com.eharmony.aloha.score.audit.take2

/**
  * Created by ryan on 12/15/16.
  */
trait TypedAuditor[K, T <: TypeCtor, A, B <: T#TC[A]] extends Auditor[K, T, A]