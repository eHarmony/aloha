package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.reflect.RefInfo

import scala.language.higherKinds

/**
  * Created by ryan on 12/16/16.
  */
trait TypeCtor {
  type TC[+A]
  def refInfo[A: RefInfo]: RefInfo[TC[A]]
}
