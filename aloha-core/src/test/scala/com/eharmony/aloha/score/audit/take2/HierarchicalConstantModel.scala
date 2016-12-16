package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 12/16/16.
  */
case class HierarchicalConstantModel[T <: TypeCtor, A, SN, N](modelId: ModelIdentity, tc: T, constant: N, auditor: Auditor[ModelIdentity, T, N], sub: Model[A, T#TC[SN]])
  extends Model[A, T#TC[N]] {
  def apply(a: A) = auditor.success(tc, modelId, constant, subValues = Seq(sub(a)))
}
