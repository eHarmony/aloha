package com.eharmony.aloha.score.audit.support

import com.eharmony.aloha.id.ModelIdentity

sealed trait Tree[+A <: Value] {
  def modelId: ModelIdentity
  def value: Option[A]
  def children: Seq[Tree[Value]]
}

object Tree {
  def apply(modelId: ModelIdentity): Tree[Nothing] = Leaf(modelId, None)
  def apply[A <: Value](modelId: ModelIdentity, value: A): Tree[A] = Leaf(modelId, Option(value))
  def apply(modelId: ModelIdentity, children: Seq[Tree[Value]]): Tree[Nothing] =
    InteriorNode(modelId, None, children)
  def apply[A <: Value](modelId: ModelIdentity, value: A, children: Seq[Tree[Value]]): Tree[A] =
    InteriorNode(modelId, Option(value), children)
}

private[support] case class Leaf[+A <: Value](modelId: ModelIdentity,
                                              value: Option[A]) extends Tree[A] {
  def children: Seq[Tree[Value]] = Nil
}

private[support] case class InteriorNode[+A <: Value](modelId: ModelIdentity,
                                                      value: Option[A],
                                                      children: Seq[Tree[Value]]) extends Tree[A]
