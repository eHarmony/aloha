package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.reflect.RefInfo

/**
  * Created by ryan on 12/15/16.
  */
object OptionTC extends TypeCtor {
  type TC[A] = Option[A]
  def refInfo[A: RefInfo] = RefInfo[Option[A]]

  def instance: this.type = this
}
