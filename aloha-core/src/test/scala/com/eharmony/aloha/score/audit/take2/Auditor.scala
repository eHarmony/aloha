package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 12/15/16.
  *
  * @tparam K The key type to be associated with the value of type `A`.
  * @tparam A The output of something that should be audited.
  */
trait Auditor[K, T <: TypeCtor, A] {

  private[aloha] def failure[S](tc: T,
                                key: K,
                                errorMsgs: => Seq[String],
                                missingVarNames: => Set[String] = Set.empty,
                                subValues: Seq[T#TC[S]] = Nil): T#TC[A]

  private[aloha] def success[S](tc: T,
                                key: K,
                                valueToAudit: A,
                                missingVarNames: => Set[String] = Set.empty,
                                subValues: Seq[T#TC[S]] = Nil,
                                prob: => Option[Double] = None): T#TC[A]

  /**
    * @return reflection information about the type of values being audited.
    */
  def refInfo: RefInfo[A]
}
