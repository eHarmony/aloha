package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.audit.support.{IntValue, StringValue, Tree, Value}
import com.eharmony.aloha.score.audit.take5.TreeAuditor.TreeType

import scala.language.existentials

/**
  * Created by ryan on 1/11/17.
  */
sealed abstract class TreeAuditor[N, +B <: Value] extends Auditor[TreeType, N, Tree[B]] {
  protected[this] def tree[V <: Value](key: ModelIdentity, value: Option[V], children: Seq[Tree[Value]]): Tree[V] = {
    value.fold[Tree[V]](
      if (children.isEmpty) Tree(key)
      else Tree(key, children)
    )(v =>
      if (children.isEmpty) Tree(key, v)
      else Tree(key, v, children)
    )
  }

//  override type OutputType[+X] = Tree[Value]
//
//  override def changeType[M: RefInfo]: Option[TreeAuditor[M, Value]] = {
//    val ri = RefInfo[M]
//    val aud = if (ri == RefInfo.Int) Option(IntTreeAuditor)
//              else if (ri == RefInfo.String) Option(StringTreeAuditor)
//              else None
//
//    aud.asInstanceOf[Option[TreeAuditor[M, Value]]]
//  }

  override private[aloha] def failure(key: ModelIdentity,
                                      errorMsgs: => Seq[String],
                                      missingVarNames: => Set[String],
                                      subValues: Seq[TreeType]): Tree[Nothing] =
    tree(key, None, subValues)
}

object TreeAuditor {
  /**
    * `TreeType` is an existential type so that `TreeAuditor`s can be easily constructed in Java.
    */
  type TreeType = Tree[V] forSome { type V <: Value }

  def intTreeAuditor: TreeAuditor[Int, IntValue] = IntTreeAuditor
  def stringTreeAuditor: TreeAuditor[String, StringValue] = StringTreeAuditor
}

object IntTreeAuditor extends TreeAuditor[Int, IntValue] {
  override private[aloha] def success(key: ModelIdentity,
                                      valueToAudit: Int,
                                      missingVarNames: => Set[String],
                                      subValues: Seq[TreeType],
                                      prob: => Option[Float]): Tree[IntValue] =
    tree(key, Option(IntValue(valueToAudit)), subValues)

  private[aloha] def unapply(value: Tree[Value]): Option[Int] = {
    value.value match {
      case Some(IntValue(v)) => Some(v)
      case _ => None
    }
  }
}

object StringTreeAuditor extends TreeAuditor[String, StringValue] {
  override private[aloha] def success(key: ModelIdentity,
                                      valueToAudit: String,
                                      missingVarNames: => Set[String],
                                      subValues: Seq[TreeType],
                                      prob: => Option[Float]): Tree[StringValue] =
    tree(key, Option(StringValue(valueToAudit)), subValues)

  private[aloha] def unapply(value: Tree[Value]): Option[String] = {
    value.value match {
      case Some(StringValue(v)) => Some(v)
      case _ => None
    }
  }
}

