package com.eharmony.aloha.audit

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/11/17.
  */
trait Auditor[/*+*/U, -N, +B <: U] {
  //type V = U

  def failure(modelId: ModelIdentity,
              errorMsgs: => Seq[String],
              missingVarNames: => Set[String],
//              subvalues: Seq[V] = Nil): B
              subvalues: Seq[U] = Nil): B

  def success(modelId: ModelIdentity,
              valueToAudit: N,
              missingVarNames: => Set[String] = Set.empty,
//              subvalues: Seq[V] = Nil,
              subvalues: Seq[U] = Nil,
              prob: => Option[Float] = None): B
}

