package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo

import com.eharmony.aloha.audit.impl.TreeAuditor.Tree

/**
  * An auditor that can audit trees
  * @tparam N the type of value at the root of the tree.
  */
case class TreeAuditor[N]() extends MorphableAuditor[Tree[_], N, Tree[N]] {

  override def changeType[M: RefInfo]: Option[MorphableAuditor[Tree[_], M, Tree[M]]] = Some(TreeAuditor[M]())

  override def failure(modelId: ModelIdentity,
                       errorMsgs: => Seq[String],
                       missingVarNames: => Set[String],
                       subvalues: Seq[Tree[_]]): Tree[N] = {
    Tree[N](modelId,
            errorMsgs = errorMsgs,
            missingVarNames = missingVarNames,
            subvalues = subvalues)
  }

  override def success(modelId: ModelIdentity,
                       valueToAudit: N,
                       missingVarNames: => Set[String],
                       subvalues: Seq[Tree[_]],
                       prob: => Option[Float]): Tree[N] = {
    Tree[N](modelId,
            value = Option(valueToAudit),
            missingVarNames = missingVarNames,
            subvalues = subvalues,
            prob = prob)
  }
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
