package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/17/17.
  */
case class IntSubmodelModel[U, S <: U, A, B <: U](modelId: ModelIdentity,
                                                  submodel: Submodel[Int, A, S],
                                                  auditor: Auditor[U, Int, B]) extends Model[A, B] {
  def apply(a: A): B = {
    val (s, oi) = submodel.subValue(a)
    (oi.fold(auditor.failure(modelId, Seq("failed"), Set.empty, Seq()))
            (i => auditor.success(modelId, i, Set.empty, Seq(s), None)))
  }
}
