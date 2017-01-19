package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/17/17.
  */
case class IntSubmodelModel[U, A, B <: U](modelId: ModelIdentity,
                                          submodel: Submodel[Int, A, U],
                                          auditor: Auditor[U, Int, B])
   extends SubmodelBase[U, Int, A, B] {

  override def subvalue(a: A): Subvalue[B, Int] = {
        val s: Subvalue[U, Int] = submodel.subvalue(a)
        (s.natural.fold(Subvalue(auditor.failure(modelId, Seq("failed"), Set.empty, Seq()), Option.empty[Int]))
                       (i => Subvalue(auditor.success(modelId, i, Set.empty, Seq(s.audited), None), Option(i))))
  }
}
