package com.eharmony.aloha.score.audit

import com.eharmony.aloha.reflect.RefInfo

/**
  * A [[MorphableAuditor]] that throws away all audit history but the most recent success
  * value or error set of errors.
  *
  * Created by ryan on 12/9/16.
  */
// TODO: Look at specialization
final case class OptionAuditor[K, V](implicit val refInfo: RefInfo[V])
extends MorphableAuditor[K, V, Option[V], OptionAuditor[K, V]] {
  override type AuditOutput[A] = Option[A]
  override def auditor[U: RefInfo]: Option[OptionAuditor[K, U]] = Option(OptionAuditor[K, U]())
  override private[aloha] def failure[C](key: K, errorMsgs: => Seq[String], missingVarNames: => Set[String], childValues: Seq[Option[C]]): Option[V] = None
  override private[aloha] def success[C](key: K, valueToAudit: V, missingVarNames: => Set[String], childValues: Seq[Option[C]], prob: => Option[Double]): Option[V] = Option(valueToAudit)
}
