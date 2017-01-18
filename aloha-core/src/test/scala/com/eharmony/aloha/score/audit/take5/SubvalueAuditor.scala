package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/18/17.
  */
case class SubvalueAuditor[U, N, +B <: U](auditor: Auditor[U, N, B])
   extends Auditor[(U, Option[N]), N, (B, Option[N])] {

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
