package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/11/17.
  */
case class ConstantModel[U, N, +B <: U](
    modelId: ModelIdentity,
    auditor: Auditor[U, N, B],
    value: Option[N]
) extends Model[Any, B] {
  def apply(ignored: Any): B = (
    value.fold(auditor.failure(modelId, Nil, Set.empty, Nil))
              (auditor.success(modelId, _, Set.empty, Nil, None))
  )
}

object ConstantModel {
  // TODO: Add some run time sanity checking for types since they don't mean anything to Java when N extends AnyVal.
  def createFromJava[U, N, B <: U](modelId: ModelIdentity, auditor: Auditor[U, N, B], value: Option[_ <: N]): ConstantModel[U, N, B] = {

    ConstantModel(modelId, auditor, value)
  }
}
