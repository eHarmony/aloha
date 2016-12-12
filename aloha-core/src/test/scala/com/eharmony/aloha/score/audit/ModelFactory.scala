package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

trait ModelFactory[N, B, MA <: MorphableAuditor[ModelId, N, B, MA]] {

  /** This is just a fake placeholder method for now */
  def createConstantModel[A](sem: Semantics[A], mId: ModelId, b: N): Either[String, Model[A, B]]
}

