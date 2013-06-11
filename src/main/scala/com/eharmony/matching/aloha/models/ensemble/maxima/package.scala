package com.eharmony.matching.aloha.models.ensemble

import com.eharmony.matching.aloha.score.basic.ModelOutput
import ModelOutput.Implicits.modelOutputOrdering
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.models.ensemble.tie.TieBreaker

package object maxima {
    type MaximaValue[+A] = (ModelOutput[A], Int)
    type MaximaList[+A] = List[MaximaValue[A]]

    object Zero {
        implicit def zero[A] = (o: ModelOutput[A]) => List((o, 0))
    }

    private[maxima] final class Maxima[A](implicit o: Ordering[ModelOutput[A]]) extends ((MaximaList[A], MaximaValue[A]) => MaximaList[A]) {
        def apply(lst: MaximaList[A], v: MaximaValue[A]) = o.compare(lst.head._1, v._1) match {
            case 0 =>          v :: lst
            case c if c > 0 => lst
            case _ =>          List(v)
        }
    }

    private[maxima] final class Minima[A](implicit o: Ordering[ModelOutput[A]]) extends ((MaximaList[A], MaximaValue[A]) => MaximaList[A]) {
        def apply(lst: MaximaList[A], v: MaximaValue[A]) = o.compare(lst.head._1, v._1) match {
            case 0 =>          v :: lst
            case c if c > 0 => List(v)
            case _ =>          lst
        }
    }

    private[maxima] final class ArgMaximaSelector[A, +B](tieBreaker: TieBreaker[A], labels: IndexedSeq[B]) extends (MaximaList[A] => ModelOutput[B]) {
        private[this] val selector = tieBreaker.andThen(_.right.map(p => labels(p._2)))
        @inline def apply(a: MaximaList[A]) = selector(a)
    }

    private[maxima] final class MaximaSelector[A](tieBreaker: TieBreaker[A]) extends (MaximaList[A] => ModelOutput[A]) {
        private[this] val selector = tieBreaker.andThen(_.right.map(p => p._1))
        @inline def apply(a: MaximaList[A]) = selector(a)
    }
}
