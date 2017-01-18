package com.eharmony.aloha.score.audit.take5

/**
  * Created by ryan on 1/18/17.
  */
trait AuditableFunction[-A, N] {
  def apply[U, B <: U](auditor: Auditor[U, N, B], a: A): B
}
