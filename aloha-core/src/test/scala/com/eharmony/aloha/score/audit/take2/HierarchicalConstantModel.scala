package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 12/16/16.
  */
case class HierarchicalConstantModel[T <: TypeCtor, -A, +SN, N](
    modelId: ModelIdentity,
    constant: N,
    sub: Model[A, T#TC[SN]],
    tc: T,
    auditor: Auditor[ModelIdentity, T, N]
) extends Model[A, T#TC[N]] {
  def apply(a: A) = {
    val sa = sub(a)
    println(s"sub($a) = $sa") // TODO: Remove this.
    auditor.success(tc, modelId, constant, subValues = Seq(sa))
  }
}

object HierarchicalConstantModel {
  def apply[T <: TypeCtor, A, SN, N](modelId: ModelIdentity, constant: N, tc: T, auditor: Auditor[ModelIdentity, T, N])(sub: Model[A, T#TC[SN]]) = {
    new HierarchicalConstantModel[T, A, SN, N](modelId, constant, sub, tc, auditor)
  }
}
