package com.eharmony.aloha.feature

trait Comparisons {

    /** Is lb < x and x < ub?
      * @param x value to be compared against lower bound and upper bound
      * @param lb lower bound (exclusive)
      * @param ub upper bound (exclusive)
      * @param o an ordering
      * @tparam A type of inputs
      * @tparam B type of ordering used for comparison.  This must be a super type of the values being compared.
      * @return true if the lb < x and x < ub; otherwise, false.
      */
    def gtLt[A, B >: A](x: A, lb: A, ub: A)(implicit o: Ordering[B]) = o.lt(lb, x) && o.lt(x, ub)

    /** Is lb < x and x <= ub?
      * @param x value to be compared against lower bound and upper bound
      * @param lb lower bound (exclusive)
      * @param ub upper bound (inclusive)
      * @param o an ordering
      * @tparam A type of inputs
      * @tparam B type of ordering used for comparison.  This must be a super type of the values being compared.
      * @return true if the lb < x and x <= ub; otherwise, false.
      */
    def gtLe[A, B >: A](x: A, lb: A, ub: A)(implicit o: Ordering[B]) = o.lt(lb, x) && o.lteq(x, ub)

    /** Is lb <= x and x < ub?
      * @param x value to be compared against lower bound and upper bound
      * @param lb lower bound (inclusive)
      * @param ub upper bound (exclusive)
      * @param o an ordering
      * @tparam A type of inputs
      * @tparam B type of ordering used for comparison.  This must be a super type of the values being compared.
      * @return true if the lb <= x and x < ub; otherwise, false.
      */
    def geLt[A, B >: A](x: A, lb: A, ub: A)(implicit o: Ordering[B]) = o.lteq(lb, x) && o.lt(x, ub)

    /** Is lb <= x and x <= ub?
      * @param x value to be compared against lower bound and upper bound
      * @param lb lower bound (inclusive)
      * @param ub upper bound (inclusive)
      * @param o an ordering
      * @tparam A type of inputs
      * @tparam B type of ordering used for comparison.  This must be a super type of the values being compared.
      * @return true if the lb <= x and x <= ub; otherwise, false.
      */
    def geLe[A, B >: A](x: A, lb: A, ub: A)(implicit o: Ordering[B]) = o.lteq(lb, x) && o.lteq(x, ub)
}

object Comparisons extends Comparisons
