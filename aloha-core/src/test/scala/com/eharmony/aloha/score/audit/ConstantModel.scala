package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

case class ConstantModel[-A, N, +B](modelId: ModelId, constant: N, auditor: Auditor[ModelId, N, B]) extends Submodel[A, N, B] {
  def apply(a: A): B = auditor.success(modelId, constant)
}
