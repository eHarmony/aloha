package com.eharmony.matching.aloha.models.ensemble.tie

import scala.util.hashing.MurmurHash3
import com.eharmony.matching.aloha.models.ensemble.maxima.MaximaList
import com.eharmony.matching.aloha.score.basic.ModelOutput

object TieBreaker {
    object TieBreakerType extends Enumeration {
        type TieBreakerType = Value
        val Hashed, Random, First = Value
    }

    import TieBreakerType._

    def apply[A](t: TieBreakerType): TieBreaker[A] = t match {
        case Hashed => new HashedTieBreaker[A]
        case Random => new RandomTieBreaker[A]
        case First => new TakeFirstTieBreaker[A]
    }
}

sealed trait TieBreaker[A] extends (MaximaList[A] => ModelOutput[(A, Int)]) {
    @inline def unwrap(p: (ModelOutput[A], Int)) = p._1.right.map(l => (l, p._2))
}

class RandomTieBreaker[A](implicit r: util.Random = new util.Random(0)) extends TieBreaker[A] {
    def apply(lst: MaximaList[A]) = {
        val n = lst.size
        if (0 == n) ModelOutput.fail("RandomTieBreaker could not select value from 0 choices.") else unwrap(lst(r.nextInt(n)))
    }
}

class HashedTieBreaker[A] extends TieBreaker[A] {
    private[this] val seed = 0
    def apply(lst: MaximaList[A]) = {
        val n = lst.size
        if (0 == n) ModelOutput.fail("HashedTieBreaker could not select value from 0 choices.")
        else if (1 == n) unwrap(lst.head)
        else unwrap(lst(math.abs(MurmurHash3.orderedHash(lst, seed)) % n))
    }
}

class TakeFirstTieBreaker[A] extends TieBreaker[A] {
    @inline def apply(lst: MaximaList[A]) =
        if (lst.nonEmpty) unwrap(lst.last)
        else ModelOutput.fail("TakeFirstTieBreaker could not select value from 0 choices.")
}
