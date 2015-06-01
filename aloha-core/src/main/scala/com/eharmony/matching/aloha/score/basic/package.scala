package com.eharmony.matching.aloha.score

package object basic {
    type ModelOutput[+B] = Either[(Seq[String], Iterable[String]), B]
    type ModelFailure = Left[(Seq[String], Iterable[String]), Nothing]
    type ModelSuccess[+B] = Right[(Seq[String], Iterable[String]), B]
}
