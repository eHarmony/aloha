package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 12/12/16.
  */
abstract class HierarchicalConstantModel[-A, SN, N, +B](override val modelId: ModelIdentity, constant: N)
       extends AuditedModel[A, N, B] {

  val auditor: Auditor[ModelIdentity, N, B]

  /**
    * Notice the output type of sub is dependent on `auditor`, specifically, on
    * `auditor`'s `AuditOutput` type constructor.  This is just what we want because
    * there's a trail from an outer model's Auditor to the inner model's Auditor.  This
    * type shows that
    */
  def sub: Model[A, auditor.AuditOutput[SN]]
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
  def apply[A, SN, N, B](mId: ModelIdentity, v: N, aud: Auditor[ModelIdentity, N, B])
                        (s: AuditedModel[A, SN, aud.AuditOutput[SN]]): HierarchicalConstantModel[A, SN, N, B] = {
    new HierarchicalConstantModel[A, SN, N, B](mId, v) {
      val auditor: aud.type = aud
      val sub: AuditedModel[A, SN, auditor.AuditOutput[SN]] = s
    }
  }
}
