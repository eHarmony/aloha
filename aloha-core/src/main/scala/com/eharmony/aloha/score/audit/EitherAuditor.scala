package com.eharmony.aloha.score.audit

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.audit.EitherAuditor.Failure

/**
  * Created by ryan on 12/12/16.
  */
final case class EitherAuditor[K, V](implicit val refInfo: RefInfo[V])
extends MorphableAuditor[K, V, Either[Failure, V], EitherAuditor[K, V]] {
  override type AuditOutput[A] = Option[A]
  override def auditor[U: RefInfo]: Option[OptionAuditor[K, U]] = Option(OptionAuditor[K, U]())
  override private[aloha] def failure[C](key: K, errorMsgs: Seq[String], missingVarNames: Set[String], childValues: Seq[Option[C]]): Either[Failure, V] = Left(Failure(errorMsgs, missingVarNames))
  override private[aloha] def success[C](key: K, valueToAudit: V, missingVarNames: Set[String], childValues: Seq[Option[C]], prob: Option[Double]): Either[Failure, V] = Right(valueToAudit)
}

object EitherAuditor {
  case class Failure(errorMsgs: Seq[String], missingVarNames: Set[String])
}
