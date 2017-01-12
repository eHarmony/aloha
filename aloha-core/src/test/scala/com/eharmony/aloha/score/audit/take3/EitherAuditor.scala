package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.audit.take3.EitherAuditor.{EitherTC, Failure, Result, Success}

/**
  * Created by ryan on 12/17/16.
  */
case class EitherAuditor[A](implicit val refInfo: RefInfo[A]) extends TypedAuditor[ModelIdentity, EitherTC, A, Result[ModelIdentity, A]] {
  /**
    * Change the type of an auditor to one of the same shape, but with a different type
    * parameter `C` instead of `A`.
    *
    * @tparam C The new type being audited.
    * @return
    */
  override private[aloha] def changeType[C: RefInfo]: Option[EitherAuditor[C]] = Option(EitherAuditor[C])
  override private[aloha] def failure[S](key: ModelIdentity, errorMsgs: => Seq[String], missingVarNames: => Set[String], subValues: Seq[Result[ModelIdentity, S]]): Failure[ModelIdentity] =
    Failure(key, errorMsgs, missingVarNames, subValues)
  override private[aloha] def success[S](key: ModelIdentity, valueToAudit: A, missingVarNames: => Set[String], subValues: Seq[Result[ModelIdentity, S]], prob: => Option[Double]): Success[ModelIdentity, A] =
    Success(key, valueToAudit, missingVarNames, subValues, prob)
}

object EitherAuditor {
  sealed trait Result[K, +V]
  case class Failure[K](key: K, errorMsgs: Seq[String], missingVarNames: Set[String], subValues: Seq[Result[K, Any]]) extends Result[K, Nothing]
  case class Success[K, +V](key: K, valueToAudit: V, missingVarNames: Set[String], subValues: Seq[Result[K, Any]], prob: Option[Double]) extends Result[K, V]

  sealed trait EitherTC extends TypeCtor1 {
    override type TC[+A] = Result[ModelIdentity, A]
    override def refInfo[A: RefInfo]: RefInfo[Result[ModelIdentity, A]] = RefInfo[Result[ModelIdentity, A]]
  }
}
