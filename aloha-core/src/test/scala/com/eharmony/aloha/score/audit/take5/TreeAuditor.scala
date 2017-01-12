package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.audit.support.{IntValue, StringValue, Tree, Value}

/**
  * Created by ryan on 1/11/17.
  */
sealed abstract class TreeAuditor[A, +B <: Tree[Value]] extends Auditor[Tree[Value], A, B] {
  protected[this] def tree[V <: Value](key: ModelIdentity, value: Option[V], children: Seq[Tree[Value]]): Tree[V] = {
    value.fold[Tree[V]](
      if (children.isEmpty) Tree(key)
      else Tree(key, children)
    )(v =>
      if (children.isEmpty) Tree(key, v)
      else Tree(key, v, children)
    )
  }

  override type OutputType[+X] = Tree[Value]
  override def changeType[C: RefInfo]: Option[Auditor[Tree[Value], C, Tree[Value]]] = None
}

object TreeAuditor {
  def javaIntTreeAuditor: TreeAuditor[Int, Tree[_ <: IntValue]] = intTreeAuditor
  def intTreeAuditor: TreeAuditor[Int, Tree[IntValue]] = IntTreeAuditor
  def stringTreeAuditor: TreeAuditor[String, Tree[StringValue]] = StringTreeAuditor

  private[this] object IntTreeAuditor extends TreeAuditor[Int, Tree[IntValue]] {
    override private[aloha] def failure(key: ModelIdentity,
                                        errorMsgs: => Seq[String],
                                        missingVarNames: => Set[String],
                                        subValues: Seq[Tree[Value]]): Tree[Nothing] = {
      tree(key, None, subValues)
    }

    override private[aloha] def success(key: ModelIdentity,
                                        valueToAudit: Int,
                                        missingVarNames: => Set[String],
                                        subValues: Seq[Tree[Value]],
                                        prob: => Option[Double]): Tree[IntValue] = {
      tree(key, Option(IntValue(valueToAudit)), subValues)
    }
  }

  private[this] object StringTreeAuditor extends TreeAuditor[String, Tree[StringValue]] {
    override private[aloha] def failure(key: ModelIdentity,
                                        errorMsgs: => Seq[String],
                                        missingVarNames: => Set[String],
                                        subValues: Seq[Tree[Value]]): Tree[Nothing] = {
      tree(key, None, subValues)
    }

    override private[aloha] def success(key: ModelIdentity,
                                        valueToAudit: String,
                                        missingVarNames: => Set[String],
                                        subValues: Seq[Tree[Value]],
                                        prob: => Option[Double]): Tree[StringValue] = {
      tree(key, Option(StringValue(valueToAudit)), subValues)
    }
  }
}
