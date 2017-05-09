//package com.eharmony.aloha.audit.impl
//
//import com.eharmony.aloha.audit.MorphableAuditor
//import com.eharmony.aloha.audit.impl.MyTreeAuditor.RootedTree
//import com.eharmony.aloha.audit.impl.MyTreeAuditor.{accumErrs, accumMissing, dontAccumErrs, dontAccumMissing}
//import com.eharmony.aloha.id.ModelIdentity
//import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
//
//import scala.collection.{breakOut, immutable => sci}
//import scala.annotation.tailrec
//
/////**
////  * An auditor that can audit trees
////  * @tparam N the type of value at the root of the tree.
////  */
////case class TreeAuditor[N](accumulateErrors: Boolean = false,
////                          accumulateMissingFeatures: Boolean = false
////) extends MorphableAuditor[TreeAuditor.Tree[_], N, TreeAuditor.Tree[N]] {
////
////  private[this] val errs = if (accumulateErrors) accumErrs else dontAccumErrs
////  private[this] val missing = if (accumulateMissingFeatures) accumMissing else dontAccumMissing
////
////  /**
////    * TreeAuditors can always change type.
////    * @tparam M a new root level type for the tree.
////    * @return a new TreeAuditor with a new root type `M`.
////    */
////  override def changeType[M: RefInfo]: Some[MorphableAuditor[TreeAuditor.Tree[_], M, TreeAuditor.Tree[M]]] =
////    Some(TreeAuditor[M](accumulateErrors, accumulateMissingFeatures))
////
////  override def failure(modelId: ModelIdentity,
////                       errorMsgs: => Seq[String] = Nil,
////                       missingVarNames: => Set[String] = Set.empty,
////                       subvalues: Seq[TreeAuditor.Tree[_]] = Nil): TreeAuditor.Tree[N] = {
////
////    TreeAuditor.Tree[N](modelId,
////            errorMsgs = errs(errorMsgs, subvalues),
////            missingVarNames = missing(missingVarNames, subvalues),
////            subvalues = subvalues.toVector)
////  }
////
////  override def success(modelId: ModelIdentity,
////                       valueToAudit: N,
////                       errorMsgs: => Seq[String] = Nil,
////                       missingVarNames: => Set[String] = Set.empty,
////                       subvalues: Seq[TreeAuditor.Tree[_]] = Nil,
////                       prob: => Option[Float]): TreeAuditor.Tree[N] =
////    TreeAuditor.Tree(modelId, errs(errorMsgs, subvalues), missing(missingVarNames, subvalues),
////         Option(valueToAudit), subvalues.toVector, prob)
////}
////
////object TreeAuditor {
////  private val accumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
////    (e, s) => (e ++ s.flatMap(t => t.errorMsgs))(breakOut)
////
////  private val dontAccumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
////    (e, _) => e.toVector
////
////  private val accumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
////    (m, s) => m ++ s.flatMap(t => t.missingVarNames)
////
////  private val dontAccumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
////    (m, _) => m
////
////  /**
////    * Type returned by a TreeAuditor.  This is represents a tree with a typed root.
////    * @tparam N natural output type of the model.
////    */
////  sealed trait Tree[+N] {
////    def modelId: ModelIdentity
////    def errorMsgs: sci.Seq[String]
////    def missingVarNames: Set[String]
////    def value: Option[N]
////    def subvalues: sci.Seq[Tree[Any]]
////    def prob: Option[Float]
////    def paths: sci.Seq[sci.Seq[Tree[Any]]]
////    def nodes: sci.Seq[Tree[Any]]
////  }
////
////  sealed trait MyTree[+U] {
////    def modelId: ModelIdentity
////    def errorMsgs: sci.Seq[String]
////    def missingVarNames: Set[String]
////    def value: Option[U]
////    def subvalues: sci.Seq[MyTree[U]]
////    def prob: Option[Float]
////    def paths: sci.Seq[sci.Seq[MyTree[U]]]
////    def nodes: sci.Seq[MyTree[U]]
////  }
////
////  sealed trait RootedTree[+U, +N <: U] extends MyTree[U] {
////    override def value: Option[N]
////  }
////
////  private[this] case class RootedTreeImpl[+U, +N <: U](modelId: ModelIdentity,
////                                                       errorMsgs: sci.Seq[String],
////                                                       missingVarNames: Set[String],
////                                                       value: Option[N],
////                                                       subvalues: sci.Seq[MyTree[U]],
////                                                       prob: Option[Float]) extends RootedTree[U, N] {
////
////    override def paths: sci.Seq[sci.Seq[MyTree[U]]] = {
////      @tailrec
////      def dfs(st: List[Vector[MyTree[U]]], done: Vector[Vector[MyTree[U]]]): Vector[Vector[MyTree[U]]] = {
////        st match {
////          case current :: tail =>
////            val subs = current.last.subvalues
////            if (subs.isEmpty)
////              dfs(tail, done :+ current)
////            else dfs(subs.map(s => current :+ s) ++: tail, done)
////          case Nil => done
////        }
////      }
////
////      dfs(List(Vector(this)), Vector.empty)
////    }
////
////    override def nodes: sci.Seq[MyTree[U]] = {
////      @tailrec
////      def dfs(st: List[MyTree[U]], done: List[MyTree[U]]): List[MyTree[U]] = {
////        st match {
////          case current :: tail =>
////            // Likely, subvalues will have size 0 or 1 most of the time.
////            dfs(current.subvalues ++: tail, current :: done)
////          case Nil => done.reverse
////        }
////      }
////
////      dfs(List(this), Nil)
////    }
////  }
////
////  private[this] case class TreeValue[+N](modelId: ModelIdentity,
////                                         errorMsgs: sci.Seq[String],
////                                         missingVarNames: Set[String],
////                                         value: Option[N],
////                                         subvalues: sci.Seq[Tree[Any]],
////                                         prob: Option[Float]) extends Tree[N] {
////
////    def paths: sci.Seq[sci.Seq[Tree[Any]]] = {
////      @tailrec
////      def dfs(st: List[Vector[Tree[Any]]], done: Vector[Vector[Tree[Any]]]): Vector[Vector[Tree[Any]]] = {
////        st match {
////          case current :: tail =>
////            val subs = current.last.subvalues
////            if (subs.isEmpty)
////              dfs(tail, done :+ current)
////            else dfs(subs.map(s => current :+ s) ++: tail, done)
////          case Nil => done
////        }
////      }
////
////      dfs(List(Vector(this)), Vector.empty)
////    }
////
////    def nodes: sci.Seq[Tree[Any]] = {
////      @tailrec
////      def dfs(st: List[Tree[Any]], done: List[Tree[Any]]): List[Tree[Any]] = {
////        st match {
////          case current :: tail =>
////            // Likely, subvalues will have size 0 or 1 most of the time.
////            dfs(current.subvalues ++: tail, current :: done)
////          case Nil => done.reverse
////        }
////      }
////
////      dfs(List(this), Nil)
////    }
////  }
////
////  object Tree {
////    def apply[N](modelId: ModelIdentity,
////                 errorMsgs: sci.Seq[String] = Nil,
////                 missingVarNames: Set[String] = Set.empty,
////                 value: Option[N] = None,
////                 subvalues: sci.Seq[Tree[Any]] = Nil,
////                 prob: Option[Float] = None): Tree[N] = {
////      TreeValue(modelId, errorMsgs, missingVarNames, value, subvalues, prob)
////    }
////  }
////}
//
//// RootedTreeAuditor
//// TreeAuditor
//case class MyTreeAuditor[U, N <: U](accumulateErrors: Boolean = false,
//                                    accumulateMissingFeatures: Boolean = false
//)(implicit uri: RefInfo[U]) extends MorphableAuditor[MyTreeAuditor.Tree[U], N, MyTreeAuditor.RootedTree[U, N]] {
//
//  private[this] val errs = if (accumulateErrors) accumErrs else dontAccumErrs
//  private[this] val missing = if (accumulateMissingFeatures) accumMissing else dontAccumMissing
//
//  /**
//    * TreeAuditors can always change type.
//    * @tparam M a new root level type for the tree.
//    * @return a new TreeAuditor with a new root type `M`.
//    */
////  override def changeType[M: RefInfo]: Option[MorphableAuditor[MyTreeAuditor.Tree[U], M, MyTreeAuditor.Tree[U]]] = {
////    if (RefInfoOps.isSubType[U, M])
////      Option(MyTreeAuditor[U](accumulateErrors, accumulateMissingFeatures).asInstanceOf[MyTreeAuditor[U, M]])
////    else None
////  }
//
//
////  override def changeType[M: RefInfo]: Option[MorphableAuditor[MyTreeAuditor.Tree[U], M, MyTreeAuditor.Tree[U]]] = {
////    ???
////  }
//
//  override def changeType[M: RefInfo]: Option[MorphableAuditor[MyTreeAuditor.Tree[U], M, MyTreeAuditor.Tree[U]]] = {
//    ???
//  }
//
//  override def failure(modelId: ModelIdentity,
//                       errorMsgs: => Seq[String],
//                       missingVarNames: => Set[String],
//                       subvalues: Seq[MyTreeAuditor.Tree[U]]): MyTreeAuditor.RootedTree[U, N] =
//    RootedTree[U, N](
//      modelId,
//      errorMsgs = errs(errorMsgs, subvalues),
//      missingVarNames = missing(missingVarNames, subvalues),
//      subvalues = subvalues.toVector
//    )
//
//  override def success(modelId: ModelIdentity,
//                       valueToAudit: N,
//                       errorMsgs: => Seq[String],
//                       missingVarNames: => Set[String],
//                       subvalues: Seq[MyTreeAuditor.Tree[U]],
//                       prob: => Option[Float]): MyTreeAuditor.RootedTree[U, N] =
//    RootedTree(
//      modelId,
//      errs(errorMsgs, subvalues),
//      missing(missingVarNames, subvalues),
//      Option(valueToAudit),
//      subvalues.toVector,
//      prob
//    )
//}
//
//
//object MyTreeAuditor {
//  private val accumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
//    (e, s) => (e ++ s.flatMap(t => t.errorMsgs))(breakOut)
//
//  private val dontAccumErrs: (Seq[String], Seq[Tree[_]]) => sci.Seq[String] =
//    (e, _) => e.toVector
//
//  private val accumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
//    (m, s) => m ++ s.flatMap(t => t.missingVarNames)
//
//  private val dontAccumMissing: (Set[String], Seq[Tree[_]]) => Set[String] =
//    (m, _) => m
//
//
//  sealed trait Tree[+U] {
//    def modelId: ModelIdentity
//    def errorMsgs: sci.Seq[String]
//    def missingVarNames: Set[String]
//    def value: Option[U]
//    def subvalues: sci.Seq[Tree[U]]
//    def prob: Option[Float]
//    def paths: sci.Seq[sci.Seq[Tree[U]]]
//    def nodes: sci.Seq[Tree[U]]
//  }
//
//  sealed trait RootedTree[+U, +N <: U] extends Tree[U] {
//    override def value: Option[N]
//  }
//
//  object RootedTree {
//    def apply[U, N <: U](modelId: ModelIdentity,
//                         errorMsgs: sci.Seq[String] = Nil,
//                         missingVarNames: Set[String] = Set.empty,
//                         value: Option[N] = None,
//                         subvalues: sci.Seq[Tree[U]] = Nil,
//                         prob: Option[Float] = None): RootedTree[U, N] = {
//      RootedTreeImpl(modelId, errorMsgs, missingVarNames, value, subvalues, prob)
//    }
//  }
//
//  private[this] case class RootedTreeImpl[+U, +N <: U](modelId: ModelIdentity,
//                                                       errorMsgs: sci.Seq[String],
//                                                       missingVarNames: Set[String],
//                                                       value: Option[N],
//                                                       subvalues: sci.Seq[Tree[U]],
//                                                       prob: Option[Float]) extends RootedTree[U, N] {
//
//    override def paths: sci.Seq[sci.Seq[Tree[U]]] = {
//      @tailrec
//      def dfs(st: List[Vector[Tree[U]]], done: Vector[Vector[Tree[U]]]): Vector[Vector[Tree[U]]] = {
//        st match {
//          case current :: tail =>
//            val subs = current.last.subvalues
//            if (subs.isEmpty)
//              dfs(tail, done :+ current)
//            else dfs(subs.map(s => current :+ s) ++: tail, done)
//          case Nil => done
//        }
//      }
//
//      dfs(List(Vector(this)), Vector.empty)
//    }
//
//    override def nodes: sci.Seq[Tree[U]] = {
//      @tailrec
//      def dfs(st: List[Tree[U]], done: List[Tree[U]]): List[Tree[U]] = {
//        st match {
//          case current :: tail =>
//            // Likely, subvalues will have size 0 or 1 most of the time.
//            dfs(current.subvalues ++: tail, current :: done)
//          case Nil => done.reverse
//        }
//      }
//
//      dfs(List(this), Nil)
//    }
//  }
//}