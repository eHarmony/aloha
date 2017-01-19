package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/18/17.
  */
//case class SubvalueAuditor[U, N, +B <: U](auditor: Auditor[U, N, B])
//   extends Auditor[Subvalue[U, N], N, Subvalue[B, N]] {
//
//  override private[aloha] def failure(key: ModelIdentity,
//                                      errorMsgs: => Seq[String],
//                                      missingVarNames: => Set[String],
//                                      subValues: Seq[Subvalue[U, N]]): Subvalue[B, N] = {
//    val s = subValues map { sv => sv.audited }
//    Subvalue(auditor.failure(key, errorMsgs, missingVarNames, s), None)
//  }
//
//  override private[aloha] def success(key: ModelIdentity,
//                                      valueToAudit: N,
//                                      missingVarNames: => Set[String],
//                                      subValues: Seq[Subvalue[U, N]],
//                                      prob: => Option[Float]): Subvalue[B, N] = {
//    val s = subValues map { sv => sv.audited }
//    Subvalue(auditor.success(key, valueToAudit, missingVarNames, s, prob), Some(valueToAudit))
//  }
//}
