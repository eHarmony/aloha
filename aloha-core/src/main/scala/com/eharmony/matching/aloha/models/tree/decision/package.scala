package com.eharmony.matching.aloha.models.tree

import com.eharmony.matching.aloha.util.EitherHelpers

package object decision extends EitherHelpers {

    private[decision] case class ErrorsAndMissing(errors: Seq[String], missing: Seq[String])

    private[decision] case class InteriorNodeResult[-A, +B](node: InteriorNode[A, B], errors: Seq[String], missing: Seq[String])

//    /** The first sequence on the left is for error messages and the second is for missing features.
//      * The right side is to report a child index down which we should branch.
//      */
//    private[decision] type NodeSelectorResult = Either[ErrorsAndMissing, Int]
//
//    /** Type alias for functions that choose adjacent nodes (or return error messages).
//      * @tparam A Input type to a model
//      */
//    private[decision] type NodeSelector[-A] = A => NodeSelectorResult

    /** Originally, NodeSelector was just a type alias for (A => Either[ErrorsAndMissing, Int]) but,
      * we want to limit the object creation for the happy path.  That is, when traversing a decision tree, no
      * unnecessary objects should be created when there are no problems.
      * @tparam A Input type to a model
      */
    private[decision] trait NodeSelector[-A] {

        /** Return:
          * -  a positive value on success
          * -  a negative index i to indicate a missing data for data at i
          * @param a input to decision tree model
          * @return
          */
        def apply(a: A): Int

        /**
          * @param a input to decision tree model
          * @param i the value returned by apply (this should be a negative number)
          * @return
          */
        def processErrorAt(a: A, i: Int): ErrorsAndMissing
    }
}
