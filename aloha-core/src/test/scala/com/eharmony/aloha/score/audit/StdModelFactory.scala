package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

case class StdModelFactory[N, B, MA <: MorphableAuditor[ModelId, N, B, MA]](
    auditor: MorphableAuditor[ModelId, N, B, MA]
)  extends ModelFactory[N, B, MA] {

  /** This is just a fake placeholder method for now */
  def createConstantModel[A](sem: Semantics[A], mId: ModelId, b: N): Either[String, Model[A, B]] =
    Right(ConstantModel(mId, b, auditor))
}
