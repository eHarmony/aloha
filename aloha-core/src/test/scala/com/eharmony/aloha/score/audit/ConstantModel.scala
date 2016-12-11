package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

case class ConstantModel[-A, B, +Y](modelId: ModelId, b: B, aud: Auditor[ModelId, B, Y]) extends Model[A, Y] {
  def apply(a: A): Y = aud.success(modelId, b)
}
