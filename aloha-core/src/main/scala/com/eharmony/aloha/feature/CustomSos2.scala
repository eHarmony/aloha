package com.eharmony.aloha.feature

trait CustomSos2 { self: DefaultPossessor =>

    @inline def csos2Fast(value: Double, knots: Int*): Iterable[(String, Double)] =
        csos2(Option(value), indSeq(knots), true)

    @inline def csos2(value: Double, knots: Int*): Iterable[(String, Double)] =
        csos2(Option(value), indSeq(knots), false)

    @inline def csos2Fast(value: Option[Double], knots: Int*): Iterable[(String, Double)] =
        csos2(value, knots, true)

    /**
      *
      * @param value
      * @param knots should contain at least two knots and knots should be
      * @return
      */
    @inline def csos2(value: Option[Double], knots: Int*): Iterable[(String, Double)] =
        csos2(value, knots, false)


    @inline private[this] def csos2(value: Option[Double], knots: Seq[Int], ignoreChecks: Boolean): Iterable[(String, Double)] =
        if (value.isEmpty) DefaultForMissingDataInReg else mapKeys(csos2I(value.get, indSeq(knots), ignoreChecks))

    @inline private[this] def mapKeys(s: Iterable[(Int, Double)]) = s.map(p => (s"=${p._1}", p._2))

    /**
      *
      * @param v
      * @param knots
      * @param ignoreChecks (default false)
      * @return
      */
    def csos2I(v: Double, knots: IndexedSeq[Int], ignoreChecks: Boolean = false): Iterable[(Int, Double)] = {
        // Sorted might be needlessly slow so short circuit if possible.
        if (!ignoreChecks && knots.size < 2 && knots != knots.sorted) {
            // TODO: Log
            Nil
        }
        else if (v >= knots.last) Seq((knots.last, 1.0))
        else if (v <= knots.head) Seq((knots.head, 1.0))
        else {
            val neighbors = findNeighbors(v, knots)
            val c = neighbors match {
                case h :: Nil => Seq((h, 1.0))
                case l :: u :: Nil =>
                    val s = (u - l).toDouble
                    Seq((l, (u - v) / s), (u, (v - l) / s))
                case _ => Nil // This should never happen b/c findNeigbors only returns lists of size 1 or 2.
            }
            c
        }
    }

    /** Find the neighbors.
      * @param v a needle
      * @param a a haystack
      * @return one neighbor if a value in a equals v and two neighbors if v does not equal some value in a.
      */
    private[this] def findNeighbors(v: Double, a: IndexedSeq[Int]) = {
        def bs(v: Double, a: IndexedSeq[Int], l: Int, r: Int): List[Int] = {
            if (r < l) List(a(r), a(l))
            else {
                val m = (l + r) / 2
                if      (a(m) > v) bs(v, a, l, m - 1)
                else if (a(m) < v) bs(v, a, m + 1, r)
                else               List(a(m))
            }
        }
        bs(v, a, 0, a.size - 1)
    }

    /** Convert to a scala.collection.IndexedSeq.
      * Because of a bug (or poor design feature) in Scala language, toIndexedSeq returns an
      * scala.collection.immutable.IndexedSeq.  We only require a collection.IndexedSeq and don't want to pay the
      * cost of conversion.
      * {{{
      * def h[A](a: A*) = a.toIndexedSeq eq (if (a.isInstanceOf[IndexedSeq[A]]) a.asInstanceOf[IndexedSeq[A]] else a.toIndexedSeq)
      * }}}
      *
      * @param a
      * @tparam A
      * @return
      */
    @inline private[this] def indSeq[A](a: Seq[A]) = if (a.isInstanceOf[IndexedSeq[A]]) a.asInstanceOf[IndexedSeq[A]] else a.toIndexedSeq
}
