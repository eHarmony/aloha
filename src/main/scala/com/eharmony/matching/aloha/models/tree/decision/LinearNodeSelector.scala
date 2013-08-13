package com.eharmony.matching.aloha.models.tree.decision

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.EitherHelpers

/** A linear time node selection algorithm that is based on applying the predicates in order to the input datum and
  * selecting the first node whose associated predicate succeeds.  If no predicate succeeds, return an error.
  *
  * @param predicates The number of predicates must equal the number of nodes
  * @tparam A input type
  */
case class LinearNodeSelector[-A](predicates: List[GenAggFunc[A, Option[Boolean]]], missingDataOk: Boolean = false)
    extends NodeSelector[A]
    with EitherHelpers {

    @inline def apply(a: A) = g(a, predicates, 0)

    /**
      * Return:
      * -  positive index i if child i should be traversed
      * -  negative index if index i has missing data
      * -  -predicates.size if no predicate is satisfied.
      * @param a input to decision tree model
      * @param p a predicate to test
      * @param i index of the predicate in the list.
      * @return
      */
    private[this] def g(a: A, p: List[GenAggFunc[A, Option[Boolean]]], i: Int): Int = p match {
        case Nil => -predicates.size - 1                               // Couldn't determine child (all data present or missingDataOk).
        case h :: t => h(a) match {
            case Some(true) => i                                       // Found child (missing data possible if missingDataOk).
            case None => if (missingDataOk) g(a, t, i + 1) else -i - 1 // If missing data OK, recurse; else, report index.
            case _ => g(a, t, i + 1)                                   // Data OK but couldn't satisfy predicate; recurse.
        }
    }

    /** Get error information.
      * '''NOTE''': This function has a linear runtime in the branching factor of the decision tree in which it is
      * employed.
      * @param a input to decision tree model
      * @param i the value returned by apply (this should be a negative number)
      * @return
      */
    def processErrorAt(a: A, i: Int): ErrorsAndMissing = {
        if (predicates.size == -i - 1) {
            val missing = predicates.flatMap(_.accessorOutputMissing(a)).distinct.sorted
            ErrorsAndMissing(Seq("No decision tree predicate satisfied. Tried: " + predicates.mkString("[", ", ", "]")), missing)
        }
        else {
            val predicate = predicates(-i - 1) // Linear-time operation in the branching factor of the tree.
            ErrorsAndMissing(Seq(s"Encountered unacceptable missing data in predicate: ${predicate.specification}"), predicate.accessorOutputMissing(a))
        }
    }
}

object LinearNodeSelector {

    /** Create a LinearNodeSelector with predicates that always handle missing data.  Map each predicate to a predicate
      * that produces a Some of the boolean returned by the original predicate.
      *
      * @param predicates a list of predicates that always produce a value.
      * @param missingDataOk is missing data dealt with (true) or considered an error (false)
      * @tparam A type of the object passed to the predicates in order to make decisions.
      * @return
      */
    def fromPredicates[A](predicates: List[GenAggFunc[A, Boolean]], missingDataOk: Boolean = false): LinearNodeSelector[A] =
        LinearNodeSelector(predicates map {_ andThenGenAggFunc {Some(_)}}, missingDataOk)
}
