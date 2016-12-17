package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 12/16/16.
  */
trait TypeCtor { self: Singleton =>
  type TC[+A]
  def refInfo[A: RefInfo]: RefInfo[TC[A]]
}

object TypeCtor {
  type Aux[C[+_]] = TypeCtor { type TC[A] = C[A] }
}
