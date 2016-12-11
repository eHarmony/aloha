package com.eharmony.aloha.score.audit

import com.eharmony.aloha.reflect.RefInfo

import scala.language.higherKinds

/**
  *
  * Created by deaktator on 12/9/16.
  *
  * @tparam K The key type to be associated with the value of type `A`.
  * @tparam A The output of something that should be audited.
  * @tparam B The output type of the auditor.  This is the type of values the Auditor produces.
  */
trait Auditor[K, A, +B] {

  /**
    * `AuditOutput` is the type constructor which represents the mapping between the
    * trait's type parameters `V` and `Out`.  So, the following would resolve correctly:
    * {{{
    * implicitly[AuditOutput[V] =:= Out] // non empty
    * }}}
    * @tparam U the type of raw value being audited.
    */
  type AuditOutput[C]

  /**
    * @return reflection information about the type of values being audited.
    */
  def refInfo: RefInfo[A]

  private[aloha] def failure[C](key: K,
                                errorMsgs: Seq[String],
                                missingVarNames: Set[String] = Set.empty,
                                childValues: Seq[AuditOutput[C]] = Nil): B

  private[aloha] def success[C](key: K,
                                valueToAudit: A,
                                missingVarNames: Set[String] = Set.empty,
                                childValues: Seq[AuditOutput[C]] = Nil,
                                prob: Option[Double] = None): B
}
