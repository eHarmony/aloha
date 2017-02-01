package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 1/30/17.
  */
case class OptionAuditor[A]() extends MorphableAuditor[Option[_], A, Option[A]] {

  override def changeType[M: RefInfo] = Some(OptionAuditor[M]())

  override def failure(key: ModelIdentity,
                       errorMsgs: => Seq[String] = Nil,
                       missingVarNames: => Set[String] = Set.empty,
                       subvalues: Seq[Option[_]] = Nil) = None

  override def success(key: ModelIdentity,
                       valueToAudit: A,
                       errorMsgs: => Seq[String] = Nil,
                       missingVarNames: => Set[String] = Set.empty,
                       subvalues: Seq[Option[_]] = Nil,
                       prob: => Option[Float] = None) = Option(valueToAudit)
}
