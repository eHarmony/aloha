package com.eharmony.aloha.models

/**
  * Created by ryan on 1/18/17.
  */
case class Subvalue[+B, +N](audited: B, natural: Option[N]) {
  def fold[A](fail: => A, success: N => A): A = natural.fold(fail)(success)
}
