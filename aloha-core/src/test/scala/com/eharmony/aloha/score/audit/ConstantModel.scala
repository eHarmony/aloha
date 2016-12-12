package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

case class ConstantModel[-A, B, +Y](modelId: ModelId, b: B, auditor: Auditor[ModelId, B, Y]) extends Submodel[A, B, Y] {
  def apply(a: A): Y = auditor.success(modelId, b)
}
