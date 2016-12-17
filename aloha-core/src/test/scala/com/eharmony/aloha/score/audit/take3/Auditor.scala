package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 12/16/16.
  */
trait Auditor[-K, T <: TypeCtor, A, +B] {

  /**
    * Change the type of an auditor to one of the same shape, but with a different type
    * parameter `C` instead of `A`.
    * @tparam C The new type being audited.
    * @return
    */
  private[aloha] def changeType[C: RefInfo]: Auditor[K, T, C, T#TC[C]]

  /**
    * @return reflection information about the type of values being audited.
    */
  def refInfo: RefInfo[A]

  private[aloha] def failure[S](key: K,
                                errorMsgs: => Seq[String],
                                missingVarNames: => Set[String] = Set.empty,
                                subValues: Seq[T#TC[S]] = Nil): B

  private[aloha] def success[S](key: K,
                                valueToAudit: A,
                                missingVarNames: => Set[String] = Set.empty,
                                subValues: Seq[T#TC[S]] = Nil,
                                prob: => Option[Double] = None): B

}
