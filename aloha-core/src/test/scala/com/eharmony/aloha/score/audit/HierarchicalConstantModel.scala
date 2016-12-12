package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

/**
  * Created by ryan on 12/12/16.
  */
abstract class HierarchicalConstantModel[-A, SN, N, +B](modelId: ModelId, constant: N)
  extends Submodel[A, N, B] {
  val auditor: Auditor[ModelId, N, B]
  val sub: Submodel[A, SN, auditor.AuditOutput[SN]]
  def apply(a: A): B = auditor.success(modelId, constant, childValues = Seq(sub(a)))
}

object HierarchicalConstantModel {

  /**
    * Construct a model that has a nested model.
    * @param mId model ID
    * @param v a constant value to return
    * @param aud an auditor
    * @param s a submodel whose auditor's `AuditOutput` type constructor is the same as the
    *          outer model's `AuditOutput` type constructor.
    * @tparam A The resulting models input type
    * @tparam SN The submodel's "''natural type''".
    * @tparam N The outer model's "''natural type''".
    * @tparam B The outer model's output type.
    * @return A HierarchicalConstantModel
    */
  def apply[A, SN, N, B](mId: ModelId, v: N, aud: Auditor[ModelId, N, B])
                        (s: Submodel[A, SN, aud.AuditOutput[SN]]): HierarchicalConstantModel[A, SN, N, B] = {
    new HierarchicalConstantModel[A, SN, N, B](mId, v) {
      val auditor: aud.type = aud
      val sub: Submodel[A, SN, auditor.AuditOutput[SN]] = s
    }
  }
}
