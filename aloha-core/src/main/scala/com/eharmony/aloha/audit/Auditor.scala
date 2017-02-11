package com.eharmony.aloha.audit

import com.eharmony.aloha.id.ModelIdentity

/**
  * API for recursive auditing.
  *
  * Created by ryan on 1/11/17.
  *
  * @tparam U The upper type bound for items being audited.
  * @tparam N The "''natural''" output type of a given model.
  * @tparam B The output type of the Auditor.  This is the same as the output type of a
  *           [[com.eharmony.aloha.models.Model]].
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

