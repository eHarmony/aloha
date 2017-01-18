package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/17/17.
  */

trait AuditableFunction[A, N] {
  def apply[U, B <: U](auditor: Auditor[U, N, B], a: A): B
}

trait AuditedModel[U, N, A, B <: U] extends Model[A, B]
                                       with AuditableFunction[A, N] {
  def auditor: Auditor[U, N, B]
  def apply(a: A): B = apply(auditor, a)
  def subValueAuditor: Auditor[(U, Option[N]), N, (B, Option[N])]
  def subValue(a: A): (B, Option[N]) = apply(subValueAuditor, a)
}

case class SubValueAuditor[U, N, B <: U](auditor: Auditor[U, N, B]) extends Auditor[(U, Option[N]), N, (B, Option[N])] {
  override private[aloha] def failure(key: ModelIdentity,
                                      errorMsgs: => Seq[String],
                                      missingVarNames: => Set[String],
                                      subValues: Seq[(U, Option[N])]) = {
    val s = subValues map { sv => sv._1 }
    (auditor.failure(key, errorMsgs, missingVarNames, s), None)
  }

  override private[aloha] def success(key: ModelIdentity,
                                      valueToAudit: N,
                                      missingVarNames: => Set[String],
                                      subValues: Seq[(U, Option[N])],
                                      prob: => Option[Float]): (B, Option[N]) = {
    val s = subValues map { sv => sv._1 }
    (auditor.success(key, valueToAudit, missingVarNames, s, prob), Some(valueToAudit))
  }
}
