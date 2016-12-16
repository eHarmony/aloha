package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.reflect.RefInfo

import scala.language.higherKinds

/**
  * A wrapper around a type constructor.  Only Scala '''object'''s can extend this because
  * of the self type.
  * Created by ryan on 12/15/16.
  */
trait TypeCtor { self: Singleton =>
  type TC[A]
  def refInfo[A: RefInfo]: RefInfo[TC[A]]
}

object TypeCtor {
  type Aux[C[_]] = TypeCtor { type TC[A] = C[A] }
}
