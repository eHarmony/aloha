package com.eharmony.aloha.score.audit.take2

/**
  * Created by ryan on 12/15/16.
  */
import com.eharmony.aloha.id.ModelIdentity

case class ConstantModel[T <: TypeCtor, A, N](modelId: ModelIdentity, constant: N, tc: T, auditor: Auditor[ModelIdentity, T, N]) extends Model[A, T#TC[N]] {
  def apply(a: A): T#TC[N] = auditor.success(tc, modelId, constant)
}
