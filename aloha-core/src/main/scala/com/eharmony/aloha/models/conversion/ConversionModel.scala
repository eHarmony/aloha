package com.eharmony.aloha.models.conversion

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.{Submodel, SubmodelBase, Subvalue}

trait ConversionModel[U, SN, N, -A, +B <: U] extends SubmodelBase[U, N, A, B] {
  def modelId: ModelIdentity
  def submodel: Submodel[SN, A, U]
  def conversion: SN => N
  def auditor: Auditor[U, N, B]


  /** Produce a score.
    * @param a an input to the model representing covariate data.
    * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
    * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
    *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
    *         sub-model scores.
    */
  def subvalue(a: A): Subvalue[B, N] = {
    val s = submodel.subvalue(a)
    (s.natural.fold(failure(Seq(s"Couldn't convert codomain because submodel ${submodel.modelId} failed."),
                            Set.empty,
                            Seq(s.audited)))
                   (sn => success(conversion(sn), subvalues = Seq(s.audited))))
  }

  override def close(): Unit = submodel.close()
}
