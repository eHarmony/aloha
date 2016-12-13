package com.eharmony.aloha.score.audit

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.audit.EitherAuditor.{Failure, Result, Success}

/**
  * Created by ryan on 12/12/16.
  */
final case class EitherAuditor[K, V](implicit val refInfo: RefInfo[V])
  extends MorphableAuditor[K, V, Result[K, V], EitherAuditor[K, V]] {
  override type AuditOutput[A] = Result[K, A]
  override def auditor[U: RefInfo]: Option[EitherAuditor[K, U]] = Option(EitherAuditor[K, U]())
//  override private[aloha] def failure[C](key: K, errorMsgs: => Seq[String], missingVarNames: => Set[String], childValues: Seq[Result]): Failure[K] = Failure(key, errorMsgs, missingVarNames, childValues)
//  override private[aloha] def success[C](key: K, valueToAudit: V, missingVarNames: => Set[String], childValues: Seq[Result], prob: => Option[Double]): Success[K, V] = Success(key, valueToAudit, missingVarNames, childValues, prob)

  override private[aloha] def failure[C](key: K, errorMsgs: => Seq[String], missingVarNames: => Set[String], childValues: Seq[Result[K, C]]): Result[K, Nothing] = Failure(key, errorMsgs, missingVarNames, childValues)
  override private[aloha] def success[C](key: K, valueToAudit: V, missingVarNames: => Set[String], childValues: Seq[Result[K, C]], prob: => Option[Double]): Success[K, V] = Success(key, valueToAudit, missingVarNames, childValues, prob)
}

//final case class EitherAuditor[K, V](implicit val refInfo: RefInfo[V])
//extends MorphableAuditor[K, V, Either[(K, Seq[String], Set[String]), (K, V)], EitherAuditor[K, V]] {
//  override type AuditOutput[A] = Either[(K, Seq[String], Set[String]), (K, A)]
//  override def auditor[U: RefInfo]: Option[EitherAuditor[K, U]] = Option(EitherAuditor[K, U]())
//  override private[aloha] def failure[C](key: K, errorMsgs: => Seq[String], missingVarNames: => Set[String], childValues: Seq[Either[(K, Seq[String], Set[String]), (K, C)]]): Either[(K, Seq[String], Set[String]), (K, V)] = Left((key, errorMsgs, missingVarNames))
//  override private[aloha] def success[C](key: K, valueToAudit: V, missingVarNames: => Set[String], childValues: Seq[Either[(K, Seq[String], Set[String]), (K, C)]], prob: => Option[Double]): Either[(K, Seq[String], Set[String]), (K, V)] = Right((key, valueToAudit))
//}

object EitherAuditor {
  sealed trait Result[K, +V]
  case class Failure[K](key: K, errorMsgs: Seq[String], missingVarNames: Set[String], childValues: Seq[Result[K, Any]]) extends Result[K, Nothing]
  case class Success[K, V](key: K, valueToAudit: V, missingVarNames: Set[String], childValues: Seq[Result[K, Any]], prob: Option[Double]) extends Result[K, V]
}