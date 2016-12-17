package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 12/16/16.
  */
trait Auditor[-K, T <: TypeCtor, A, +B] {

  private[aloha] def failure[S](tc: T,
                                key: K,
                                errorMsgs: => Seq[String],
                                missingVarNames: => Set[String] = Set.empty,
                                subValues: Seq[T#TC[S]] = Nil): B

  private[aloha] def success[S](tc: T,
                                key: K,
                                valueToAudit: A,
                                missingVarNames: => Set[String] = Set.empty,
                                subValues: Seq[T#TC[S]] = Nil,
                                prob: => Option[Double] = None): B

  /**
    * @return reflection information about the type of values being audited.
    */
  def refInfo: RefInfo[A]
}
