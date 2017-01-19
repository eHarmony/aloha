package com.eharmony.aloha.score.audit.take5

/**
  * Created by ryan on 1/11/17.
  */
import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/11/17.
  */
case class HierarchicalConstantModel[U, SN, N, -A, B <: U](
    modelId: ModelIdentity,
    constant: N,
    submodel: Submodel[SN, A, U],
    auditor: Auditor[U, N, B]
) extends SubmodelBase[U, N, A, B] {

  override def subvalue(a: A): Subvalue[B, N] = {
    val s: Subvalue[U, SN] = submodel.subvalue(a)
    (s.natural.fold(Subvalue(auditor.failure(modelId, Nil, Set.empty, Nil), Option.empty[N]))
                   (_ => Subvalue(auditor.success(modelId, constant, Set.empty, Seq(s.audited), None), Option(constant))))
  }
}

object HierarchicalConstantModel {
  def createFromJava[U, SN, N, A, B <: U](
      modelId: ModelIdentity,
      constant: N,
      submodel: Submodel[SN, A, _ <: U],
      auditor: Auditor[U, N, B]): HierarchicalConstantModel[U, SN, N, A, B] = {
    HierarchicalConstantModel(modelId, constant, submodel, auditor)
  }
}