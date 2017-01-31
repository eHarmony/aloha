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
                       errorMsgs: => Seq[String],
                       missingVarNames: => Set[String],
                       subvalues: Seq[Option[_]]) = None

  override def success(key: ModelIdentity,
                       valueToAudit: A,
                       missingVarNames: => Set[String],
                       subvalues: Seq[Option[_]],
                       prob: => Option[Float]) = Option(valueToAudit)
}
