package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

/**
  * Created by ryan on 12/12/16.
  */
case class HierarchicalConstantModel[-A, B, S, S1, +Y](modelId: ModelId, b: B, auditor: Auditor[ModelId, B, Y], sub: Submodel[A, S, S1])
extends Submodel[A, B, Y] {
  def apply(a: A): Y = {
    val s = sub(a).asInstanceOf[auditor.AuditOutput[S]]
    auditor.success(modelId, b, childValues = Seq(s))
  }
}
