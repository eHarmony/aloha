package com.eharmony.aloha.score.audit.take5
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

import scala.language.existentials

/**
  * Created by ryan on 1/12/17.
  */
case class OptionAuditor[A]() extends Auditor[Option[_], A, Option[A]] {
  override type OutputType[+B] = Option[B]
  override def changeType[M: RefInfo]: Option[OptionAuditor[M]] = Option(OptionAuditor[M]())

  override private[aloha] def failure(key: ModelIdentity,
                                      errorMsgs: => Seq[String],
                                      missingVarNames: => Set[String],
                                      subValues: Seq[Option[_]]) = None

  override private[aloha] def success(key: ModelIdentity,
                                      valueToAudit: A,
                                      missingVarNames: => Set[String],
                                      subValues: Seq[Option[_]],
                                      prob: => Option[Double]) = Option(valueToAudit)
}
