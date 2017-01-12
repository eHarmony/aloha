package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 1/11/17.
  */
/**
  * Created by ryan on 1/9/17.
  */
trait Auditor[U, -N, +B <: U] {
  type OutputType[+X] <: U

  def changeType[M: RefInfo]: Option[Auditor[U, M, OutputType[M]]]

  private[aloha] def failure(key: ModelIdentity,
                             errorMsgs: => Seq[String],
                             missingVarNames: => Set[String],
                             subValues: Seq[U]): B

  private[aloha] def success(key: ModelIdentity,
                             valueToAudit: N,
                             missingVarNames: => Set[String],
                             subValues: Seq[U],
                             prob: => Option[Double]): B
}

