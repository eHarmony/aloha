package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.id.ModelIdentity

/**
  *
  * @param modelId
  * @param constant
  * @param sub
  * @param auditor
  * @tparam T
  * @tparam SN
  * @tparam N
  * @tparam A
  * @tparam B
  */
// TODO: fill in comments.
case class HierarchicalConstantModel[T <: TypeCtor, SN, N, -A, +B <: T#TC[N]] private(
    modelId: ModelIdentity,
    constant: N,
    sub: Model[A, T#TC[SN]],
    auditor: Auditor[ModelIdentity, T, N, B]
 ) extends AuditedModel[T, N, A, B] {
  def apply(a: A): B = auditor.success(modelId, constant, subValues = Seq(sub(a)))
}

object HierarchicalConstantModel {
  def apply[T <: TypeCtor, SN, SB <: T#TC[SN], N, A, B <: T#TC[N]](modelId: ModelIdentity, constant: N, auditor: Auditor[ModelIdentity, T, N, B])(sub: AuditedModel[T, SN, A, SB]): HierarchicalConstantModel[T, SN, N, A, B] =
    new HierarchicalConstantModel[T, SN, N, A, B](modelId, constant, sub.typeConstructed, auditor)

  def createFromJava[T <: TypeCtor, SN, SB <: T#TC[SN], N, A, B <: T#TC[N]](modelId: ModelIdentity, constant: N, auditor: Auditor[ModelIdentity, T, N, B], sub: AuditedModel[T, SN, A, SB]): HierarchicalConstantModel[T, SN, N, A, B] =
    apply[T, SN, SB, N, A, B](modelId, constant, auditor)(sub)
}
