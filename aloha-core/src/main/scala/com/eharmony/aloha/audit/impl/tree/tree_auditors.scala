package com.eharmony.aloha.audit.impl.tree

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}

import scala.annotation.tailrec
import scala.collection.{breakOut, immutable => sci}

sealed trait Tree[+U] {
  def modelId: ModelIdentity
  def errorMsgs: sci.Seq[String]
  def missingVarNames: Set[String]
  def value: Option[U]
  def subvalues: sci.Seq[Tree[U]]
  def prob: Option[Float]
  def paths: sci.Seq[sci.Seq[Tree[U]]]
  def nodes: sci.Seq[Tree[U]]
}

sealed trait RootedTree[+U, +N <: U] extends Tree[U] {
  override def value: Option[N]
}

trait TreePathOps[+U] { self: Tree[U] =>
  override def paths: sci.Seq[sci.Seq[Tree[U]]] = {
    @tailrec
    def dfs(st: List[Vector[Tree[U]]], done: Vector[Vector[Tree[U]]]): Vector[Vector[Tree[U]]] = {
      st match {
        case current :: tail =>
          val subs = current.last.subvalues
          if (subs.isEmpty)
            dfs(tail, done :+ current)
          else dfs(subs.map(s => current :+ s) ++: tail, done)
        case Nil => done
      }
    }

    dfs(List(Vector(this)), Vector.empty)
  }

  override def nodes: sci.Seq[Tree[U]] = {
    @tailrec
    def dfs(st: List[Tree[U]], done: List[Tree[U]]): List[Tree[U]] = {
      st match {
        case current :: tail =>
          // Likely, subvalues will have size 0 or 1 most of the time.
          dfs(current.subvalues ++: tail, current :: done)
        case Nil => done.reverse
      }
    }

    dfs(List(this), Nil)
  }
}

private[tree] case class TreeImpl[+U](
  modelId: ModelIdentity,
  errorMsgs: sci.Seq[String],
  missingVarNames: Set[String],
  value: Option[U],
  subvalues: sci.Seq[Tree[U]],
  prob: Option[Float]
) extends Tree[U] with TreePathOps[U]


private[tree] case class RootedTreeImpl[+U, +N <: U](
    modelId: ModelIdentity,
    errorMsgs: sci.Seq[String],
    missingVarNames: Set[String],
    value: Option[N],
    subvalues: sci.Seq[Tree[U]],
    prob: Option[Float]
) extends RootedTree[U, N] with TreePathOps[U]

case class TreeAuditor[U, N](accumulateErrors: Boolean = false,
                             accumulateMissingFeatures: Boolean = false
)(implicit uri: RefInfo[U], toU: N => U) extends MorphableAuditor[Tree[U], N, Tree[U]] {
  import TreeAuditor._

  private[this] val errs = if (accumulateErrors) accumErrs else dontAccumErrs
  private[this] val missing = if (accumulateMissingFeatures) accumMissing else dontAccumMissing


  override def changeType[M: RefInfo]: Option[MorphableAuditor[Tree[U], M, Tree[U]]] = {
    if (RefInfoOps.isSubType[M, U])
      Option(TreeAuditor[U, M](accumulateErrors, accumulateMissingFeatures)(uri, _.asInstanceOf[U]))
    else None
  }

  override def failure(modelId: ModelIdentity,
                       errorMsgs: => Seq[String],
                       missingVarNames: => Set[String],
                       subvalues: Seq[Tree[U]]): Tree[U] = {
    TreeImpl[U](
      modelId,
      errs(errorMsgs, subvalues),
      missing(missingVarNames, subvalues),
      None,
      subvalues.toVector,
      None
    )
  }

  override def success(modelId: ModelIdentity,
                       valueToAudit: N,
                       errorMsgs: => Seq[String],
                       missingVarNames: => Set[String],
                       subvalues: Seq[Tree[U]],
                       prob: => Option[Float]): Tree[U] =
    TreeImpl(
      modelId,
      errs(errorMsgs, subvalues),
      missing(missingVarNames, subvalues),
      Option(toU(valueToAudit)),
      subvalues.toVector,
      prob
    )
}

object TreeAuditor {
  private[tree] val accumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
    (e, s) => (e ++ s.flatMap(t => t.errorMsgs))(breakOut)

  private[tree] val dontAccumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
    (e, _) => e.toVector

  private[tree] val accumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
    (m, s) => m ++ s.flatMap(t => t.missingVarNames)

  private[tree] val dontAccumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
    (m, _) => m
}

case class RootedTreeAuditor[U: RefInfo, N <: U] private(accumulateErrors: Boolean,
                                                         accumulateMissingFeatures: Boolean
) extends MorphableAuditor[Tree[U], N, RootedTree[U, N]] {
  import TreeAuditor._

  private[this] val errs = if (accumulateErrors) accumErrs else dontAccumErrs
  private[this] val missing = if (accumulateMissingFeatures) accumMissing else dontAccumMissing

  override def changeType[M: RefInfo]: Option[MorphableAuditor[Tree[U], M, Tree[U]]] = {
    if (RefInfoOps.isSubType[M, U])
      Option(TreeAuditor[U, M](accumulateErrors, accumulateMissingFeatures)(RefInfo[U], _.asInstanceOf[U]))
    else None
  }

  override def failure(modelId: ModelIdentity,
                       errorMsgs: => Seq[String],
                       missingVarNames: => Set[String],
                       subvalues: Seq[Tree[U]]): RootedTree[U, N] =
    RootedTreeImpl[U, N](
      modelId,
      errs(errorMsgs, subvalues),
      missing(missingVarNames, subvalues),
      None,
      subvalues.toVector,
      None
    )

  override def success(modelId: ModelIdentity,
                       valueToAudit: N,
                       errorMsgs: => Seq[String],
                       missingVarNames: => Set[String],
                       subvalues: Seq[Tree[U]],
                       prob: => Option[Float]): RootedTree[U, N] = {
    RootedTreeImpl[U, N](
      modelId,
      errs(errorMsgs, subvalues),
      missing(missingVarNames, subvalues),
      Option(valueToAudit),
      subvalues.toVector,
      prob
    )
  }
}

object RootedTreeAuditor {
  def noUpperBound[N](accumulateErrors: Boolean = false,
               accumulateMissingFeatures: Boolean = false): RootedTreeAuditor[Any, N] =
    RootedTreeAuditor[Any, N](accumulateErrors, accumulateMissingFeatures)

  def withUpperBound[U: RefInfo, N <: U](accumulateErrors: Boolean = false,
                                accumulateMissingFeatures: Boolean = false): RootedTreeAuditor[Any, N] =
    RootedTreeAuditor[Any, N](accumulateErrors, accumulateMissingFeatures)
}