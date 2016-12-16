package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 12/15/16.
  */
case class OptionAuditor[A](implicit val refInfo: RefInfo[A]) extends TypedAuditor[ModelIdentity, TypeCtor.Aux[Option], A, Option[A]]{
  override private[aloha] def failure[S](tc: TypeCtor.Aux[Option], key: ModelIdentity, errorMsgs: => Seq[String], missingVarNames: => Set[String], subValues: Seq[TypeCtor.Aux[Option]#TC[S]]): Option[A] = None
  override private[aloha] def success[S](tc: TypeCtor.Aux[Option], key: ModelIdentity, valueToAudit: A, missingVarNames: => Set[String], subValues: Seq[TypeCtor.Aux[Option]#TC[S]], prob: => Option[Double]): Option[A] = Option(valueToAudit)
}
