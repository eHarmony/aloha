package com.eharmony.matching.aloha.score.basic

class ModelOutputOrdering[B: Ordering] extends Ordering[ModelOutput[B]] {
    private val o = Ordering[B]
    def compare(x: ModelOutput[B], y: ModelOutput[B]) =
        if (x.isLeft)
            if (y.isLeft) 0 else 1
        else if (y.isLeft) -1
        else o.compare(x.right.get, y.right.get)
}
