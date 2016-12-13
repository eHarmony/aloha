package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelIdentity

case class StdModelFactory() extends ModelFactory {

  /** This is just a fake placeholder method for now */
  override def createConstantModel[A, N, B, MA <: MorphableAuditor[ModelIdentity, N, B, MA]](
      sem: Semantics[A],
      auditor: MorphableAuditor[ModelIdentity, N, B, MA],
      mId: ModelIdentity,
      constant: N): Either[String, Model[A, B]] =
    Right(ConstantModel(mId, constant, auditor))
}
