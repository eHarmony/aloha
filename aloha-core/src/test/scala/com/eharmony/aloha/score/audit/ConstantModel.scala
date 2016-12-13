package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelIdentity

case class ConstantModel[-A, N, +B](modelId: ModelIdentity, constant: N, auditor: Auditor[ModelIdentity, N, B])
extends AuditedModel[A, N, B] {
  def apply(a: A): B = auditor.success(modelId, constant)
}
