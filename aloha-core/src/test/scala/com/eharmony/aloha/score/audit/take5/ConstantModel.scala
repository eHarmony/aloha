package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/11/17.
  */
case class ConstantModel[U, N, +B <: U](
    modelId: ModelIdentity,
    auditor: Auditor[U, N, B],
    value: Option[N]
) extends AuditedModel[U, N, Any, B] {
  def apply(ignored: Any): B = (
    value.fold(auditor.failure(modelId, Nil, Set.empty, Nil))
              (auditor.success(modelId, _, Set.empty, Nil, None))
  )
}

object ConstantModel {
  def createFromJava[U, N, B <: U](modelId: ModelIdentity, auditor: Auditor[U, N, B], value: Option[_ <: N]): ConstantModel[U, N, B] =
    ConstantModel(modelId, auditor, value)
}
