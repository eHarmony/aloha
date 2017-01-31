package com.eharmony.aloha.models

import com.eharmony.aloha.audit.Auditor

/**
  * Created by ryan on 1/18/17.
  */
trait SubmodelBase[U, N, -A, +B <: U] extends Submodel[N, A, B]
                                         with Model[A, B] {
  def auditor: Auditor[U, N, B]

  final def apply(a: A): B = subvalue(a).audited

  protected[this] def failure(errorMsgs: => Seq[String],
                              missingVarNames: => Set[String],
                              subvalues: Seq[U] = Nil): Subvalue[B, N] =
    Subvalue(auditor.failure(modelId, errorMsgs, missingVarNames, subvalues), None)

  protected[this] def success(naturalValue: N,
                              missingVarNames: => Set[String] = Set.empty,
                              subvalues: Seq[U] = Nil,
                              prob: => Option[Float] = None): Subvalue[B, N] =
    Subvalue(auditor.success(modelId, naturalValue, missingVarNames, subvalues, prob), Some(naturalValue))

  def close(): Unit = ()
}
