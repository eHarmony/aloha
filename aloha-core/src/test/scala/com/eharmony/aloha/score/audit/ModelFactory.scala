package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelIdentity

trait ModelFactory {
  def createConstantModel[A, N, B, MA <: MorphableAuditor[ModelIdentity, N, B, MA]](
    sem: Semantics[A],
    auditor: MorphableAuditor[ModelIdentity, N, B, MA],
    mId: ModelIdentity,
    constant: N): Either[String, Model[A, B]]
}

