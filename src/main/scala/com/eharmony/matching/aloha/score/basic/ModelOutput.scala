package com.eharmony.matching.aloha.score.basic

import com.eharmony.matching.aloha.score.conversions.ScoreConverter

object ModelOutput {
    @inline def isFail[B](mo: ModelOutput[B]) = mo.isLeft
    @inline def isSuccess[B](mo: ModelOutput[B]) = mo.isRight
    @inline def apply[B: ScoreConverter](b: B): ModelSuccess[B] = Right(b)
    @inline def fail(s: String*): ModelFailure = Left((s, Nil))
    @inline def fail(errors: Seq[String], missingFields: Iterable[String]): ModelFailure = Left((errors, missingFields))
    object Implicits {
        implicit def modelOutputOrdering[B: Ordering]: Ordering[ModelOutput[B]] = new ModelOutputOrdering[B]
    }
}

