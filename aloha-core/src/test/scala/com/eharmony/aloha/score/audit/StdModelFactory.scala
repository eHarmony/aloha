package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

case class StdModelFactory[B, Y, MA <: MorphableAuditor[ModelId, B, Y, MA]](
    auditor: MorphableAuditor[ModelId, B, Y, MA]
)  extends ModelFactory[B, Y, MA] {

  /** This is just a fake placeholder method for now */
  def createConstantModel[A](sem: Semantics[A], mId: ModelId, b: B): Either[String, Model[A, Y]] =
    Right(ConstantModel(mId, b, auditor))
}
