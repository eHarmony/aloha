package com.eharmony.aloha.audit.impl

import com.eharmony.aloha.audit.MorphableAuditor
import com.eharmony.aloha.audit.impl.TreeAuditor.{Tree, accumErrs, accumMissing, dontAccumErrs, dontAccumMissing}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import scala.collection.{immutable => sci}
import scala.collection.breakOut

import scala.annotation.tailrec

/**
  * An auditor that can audit trees
  * @tparam N the type of value at the root of the tree.
  */
case class TreeAuditor[N](accumulateErrors: Boolean = false,
                          accumulateMissingFeatures: Boolean = false
) extends MorphableAuditor[Tree[_], N, Tree[N]] {

  private[this] val errs = if (accumulateErrors) accumErrs else dontAccumErrs
  private[this] val missing = if (accumulateMissingFeatures) accumMissing else dontAccumMissing

  /**
    * TreeAuditors can always change type.
    * @tparam M a new root level type for the tree.
    * @return a new TreeAuditor with a new root type `M`.
    */
  override def changeType[M: RefInfo]: Some[MorphableAuditor[Tree[_], M, Tree[M]]] =
    Some(TreeAuditor[M](accumulateErrors, accumulateMissingFeatures))

  override def failure(modelId: ModelIdentity,
                       errorMsgs: => Seq[String] = Nil,
                       missingVarNames: => Set[String] = Set.empty,
                       subvalues: Seq[Tree[_]] = Nil): Tree[N] = {

    Tree[N](modelId,
            errorMsgs = errs(errorMsgs, subvalues),
            missingVarNames = missing(missingVarNames, subvalues),
            subvalues = subvalues.toVector)
  }

  override def success(modelId: ModelIdentity,
                       valueToAudit: N,
                       errorMsgs: => Seq[String] = Nil,
                       missingVarNames: => Set[String] = Set.empty,
                       subvalues: Seq[Tree[_]] = Nil,
                       prob: => Option[Float]): Tree[N] =
    Tree(modelId, errs(errorMsgs, subvalues), missing(missingVarNames, subvalues),
         Option(valueToAudit), subvalues.toVector, prob)
}

object TreeAuditor {
  private val accumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
    (e, s) => (e ++ s.flatMap(t => t.errorMsgs))(breakOut)

  private val dontAccumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
    (e, _) => e.toVector

  private val accumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
    (m, s) => m ++ s.flatMap(t => t.missingVarNames)

  private val dontAccumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
    (m, _) => m

  /**
    * Type returned by a TreeAuditor.  This is represents a tree with a typed root.
    * @tparam N natural output type of the model.
    */
  sealed trait Tree[+N] {
    def modelId: ModelIdentity
    def errorMsgs: sci.Seq[String]
    def missingVarNames: Set[String]
    def value: Option[N]
    def subvalues: sci.Seq[Tree[Any]]
    def prob: Option[Float]
    def paths: sci.Seq[sci.Seq[Tree[Any]]]
    def nodes: sci.Seq[Tree[Any]]
  }

  private[this] case class TreeValue[+N](modelId: ModelIdentity,
                                         errorMsgs: sci.Seq[String],
                                         missingVarNames: Set[String],
                                         value: Option[N],
                                         subvalues: sci.Seq[Tree[Any]],
                                         prob: Option[Float]) extends Tree[N] {

    def paths: sci.Seq[sci.Seq[Tree[Any]]] = {
      @tailrec
      def dfs(st: List[Vector[Tree[Any]]], done: Vector[Vector[Tree[Any]]]): Vector[Vector[Tree[Any]]] = {
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

    def nodes: sci.Seq[Tree[Any]] = {
      @tailrec
      def dfs(st: List[Tree[Any]], done: List[Tree[Any]]): List[Tree[Any]] = {
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

  object Tree {
    def apply[N](modelId: ModelIdentity,
                 errorMsgs: sci.Seq[String] = Nil,
                 missingVarNames: Set[String] = Set.empty,
                 value: Option[N] = None,
                 subvalues: sci.Seq[Tree[Any]] = Nil,
                 prob: Option[Float] = None): Tree[N] = {
      TreeValue(modelId, errorMsgs, missingVarNames, value, subvalues, prob)
    }
  }
}
