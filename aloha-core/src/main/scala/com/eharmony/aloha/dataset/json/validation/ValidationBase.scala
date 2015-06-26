package com.eharmony.aloha.dataset.json.validation

import scala.collection.SeqView

trait ValidationBase {
    protected[this] final def reportDuplicates[A, K](label: String, xs: SeqView[A, Seq[A]])(criteria: A => K) = {
        val dup = findDupicates(xs)(criteria)
        Option(dup.nonEmpty) collect { case true =>  s"$label: $dup." }
    }

    protected[this] final def reportDuplicates[A](label: String, xs: Seq[A]) = {
        Option(xs.nonEmpty) collect { case true =>  s"$label: $xs." }
    }

    /**
     * Find duplicates in xs based on some criteria.
     * @param xs where to look for duplicates
     * @param criteria the criteria used to determine duplicates.
     * @tparam A type of
     * @tparam K
     * @return
     */
    protected[this] final def findDupicates[A, K](xs: SeqView[A, Seq[A]])(criteria: A => K) =
        xs.groupBy(criteria).collect{case (k, v) if v.size > 1 => k }(collection.breakOut)
}
