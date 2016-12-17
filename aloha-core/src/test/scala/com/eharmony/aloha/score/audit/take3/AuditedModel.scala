package com.eharmony.aloha.score.audit.take3

/**
  * Created by ryan on 12/16/16.
  */
trait AuditedModel[T <: TypeCtor, N, -A, +B <: T#TC[N]] extends Model[A, B] {
  def typeConstructed: AuditedModel[T, N, A, T#TC[N]] =
    this.asInstanceOf[AuditedModel[T, N, A, T#TC[N]]]
}
