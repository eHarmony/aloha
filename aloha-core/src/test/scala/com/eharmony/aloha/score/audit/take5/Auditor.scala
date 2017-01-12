package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 1/11/17.
  */
/**
  * Created by ryan on 1/9/17.
  */
trait Auditor[U, A, +B <: U] {
  type OutputType[+X] <: U

  def changeType[C: RefInfo]: Option[Auditor[U, C, OutputType[C]]]

  private[aloha] def failure(key: ModelIdentity,
                             errorMsgs: => Seq[String],
                             missingVarNames: => Set[String],
                             subValues: Seq[U]): B

  private[aloha] def success(key: ModelIdentity,
                             valueToAudit: A,
                             missingVarNames: => Set[String],
                             subValues: Seq[U],
                             prob: => Option[Double]): B
}

