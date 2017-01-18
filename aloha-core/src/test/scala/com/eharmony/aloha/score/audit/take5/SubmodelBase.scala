package com.eharmony.aloha.score.audit.take5

/**
  * Created by ryan on 1/18/17.
  */
trait SubmodelBase[U, N, -A, +B <: U] extends Submodel[N, A, B]
                                         with AuditableFunction[A, N] {
  protected[this] def auditor: Auditor[U, N, B]
  @transient private[this] lazy val subValueAuditor: SubvalueAuditor[U, N, B] = SubvalueAuditor(auditor)
  final def subValue(a: A): (B, Option[N]) = apply(subValueAuditor, a)
}
