package com.eharmony.aloha.score.audit.take3

/**
  * Created by ryan on 12/16/16.
  */
trait TypedAuditor[K, T <: TypeCtor, A, B <: T#TC[A]] extends Auditor[K, T, A, B]