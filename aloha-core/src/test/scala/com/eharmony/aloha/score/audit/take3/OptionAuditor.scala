package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.audit.take3.OptionAuditor.OptionTC



/**
  * Created by ryan on 12/16/16.
  */
case class OptionAuditor[A](implicit val refInfo: RefInfo[A]) extends TypedAuditor[ModelIdentity, OptionTC, A, Option[A]]{
  override private[aloha] def changeType[C: RefInfo]: Option[OptionAuditor[C]] = Option(OptionAuditor[C])
  override private[aloha] def failure[S](key: ModelIdentity, errorMsgs: => Seq[String], missingVarNames: => Set[String], subValues: Seq[Option[S]]): Option[A] = None
  override private[aloha] def success[S](key: ModelIdentity, valueToAudit: A, missingVarNames: => Set[String], subValues: Seq[Option[S]], prob: => Option[Double]): Option[A] = Option(valueToAudit)
}

object OptionAuditor {
  sealed trait OptionTC extends TypeCtor1 {
    override type TC[+A] = Option[A]
    override def refInfo[A: RefInfo]: RefInfo[Option[A]] = RefInfo[Option[A]]
  }
}
