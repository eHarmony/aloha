package com.eharmony.aloha.models.vw.jni

/**
  * Created by sahil-goyal on 10/25/16.
  */
sealed trait VwEvaluator[-A, -I, +B] extends ((A, I) => B)

final case class DefaultEvaluator[-A, -I, +B](f: (A, I) => B) extends VwEvaluator[A, I, B] {

  def apply(v1: A, v2: I): B = f(v1, v2)

}

final case class ContextualEvaluator[-A, -I, C, +B](ctx: A => C, f: (A, C, I) => B) extends VwEvaluator[A, I, B] {

  def apply(v1: A, v2: I): B = f(v1, ctx(v1), v2)

}
