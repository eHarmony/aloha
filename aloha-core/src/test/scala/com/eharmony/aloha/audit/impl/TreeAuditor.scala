package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

import com.eharmony.aloha.audit.impl.TreeAuditor.Tree

/**
  * An auditor that can audit trees
  * @tparam N the type of value at the root of the tree.
  */
case class TreeAuditor[N](accumulateErrors: Boolean = false,
                          accumulateMissingFeatures: Boolean = false
) extends MorphableAuditor[Tree[_], N, Tree[N]] {

  private[this] val errs = if (accumulateErrors)
    (e: Seq[String], s: Seq[Tree[_]]) => e ++ s.flatMap(t => t.errorMsgs)
  else (e: Seq[String], s: Seq[Tree[_]]) => e

  private[this] val missing = if (accumulateMissingFeatures)
    (m: Set[String], s: Seq[Tree[_]]) => m ++ s.flatMap(t => t.missingVarNames)
  else (m: Set[String], s: Seq[Tree[_]]) => m

  override def changeType[M: RefInfo]: Option[MorphableAuditor[Tree[_], M, Tree[M]]] =
    Some(TreeAuditor[M](accumulateErrors, accumulateMissingFeatures))

  override def failure(modelId: ModelIdentity,
                       errorMsgs: => Seq[String] = Nil,
                       missingVarNames: => Set[String] = Set.empty,
                       subvalues: Seq[Tree[_]] = Nil): Tree[N] = {

    Tree[N](modelId,
            errorMsgs = errs(errorMsgs, subvalues),
            missingVarNames = missing(missingVarNames, subvalues),
            subvalues = subvalues)
  }

  override def success(modelId: ModelIdentity,
                       valueToAudit: N,
                       errorMsgs: => Seq[String] = Nil,
                       missingVarNames: => Set[String] = Set.empty,
                       subvalues: Seq[Tree[_]] = Nil,
                       prob: => Option[Float]): Tree[N] =
    Tree(modelId, errs(errorMsgs, subvalues), missing(missingVarNames, subvalues), Option(valueToAudit), subvalues, prob)
}

object TreeAuditor {
  sealed trait Tree[+N] {
    def modelId: ModelIdentity
    def errorMsgs: Seq[String]
    def missingVarNames: Set[String]
    def value: Option[N]
    def subvalues: Seq[Tree[Any]]
    def prob: Option[Float]
  }

  private[this] case class TreeValue[+N](modelId: ModelIdentity,
                                         errorMsgs: Seq[String] = Nil,
                                         missingVarNames: Set[String] = Set.empty,
                                         value: Option[N] = None,
                                         subvalues: Seq[Tree[Any]] = Nil,
                                         prob: Option[Float] = None) extends Tree[N]

  object Tree {
    def apply[N](modelId: ModelIdentity,
                 errorMsgs: Seq[String] = Nil,
                 missingVarNames: Set[String] = Set.empty,
                 value: Option[N] = None,
                 subvalues: Seq[Tree[Any]] = Nil,
                 prob: Option[Float] = None): Tree[N] = {
      TreeValue(modelId, errorMsgs, missingVarNames, value, subvalues, prob)
    }
  }
}
