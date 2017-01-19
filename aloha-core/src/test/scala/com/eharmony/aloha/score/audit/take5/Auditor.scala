package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/11/17.
  */
trait Auditor[U, N, +B <: U] {
  private[aloha] def failure(key: ModelIdentity,
                             errorMsgs: => Seq[String],
                             missingVarNames: => Set[String],
                             subvalues: Seq[U]): B

  private[aloha] def success(key: ModelIdentity,
                             valueToAudit: N,
                             missingVarNames: => Set[String],
                             subvalues: Seq[U],
                             prob: => Option[Float]): B
}

