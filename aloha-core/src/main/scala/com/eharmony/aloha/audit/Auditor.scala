package com.eharmony.aloha.audit

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/11/17.
  */
trait Auditor[U, -N, +B <: U] {
  def failure(modelId: ModelIdentity,
              errorMsgs: => Seq[String] = Nil,
              missingVarNames: => Set[String] = Set.empty,
              subvalues: Seq[U] = Nil): B

  def success(modelId: ModelIdentity,
              valueToAudit: N,
              errorMsgs: => Seq[String] = Nil,
              missingVarNames: => Set[String] = Set.empty,
              subvalues: Seq[U] = Nil,
              prob: => Option[Float] = None): B
}

