package com.eharmony.aloha.models.vw.jni

/**
  * Created by sahil-goyal on 10/25/16.
  */
sealed trait VwEvaluator[-A, -I, +B] extends ((A, I) => B)

final case class DefaultEvaluator[-I, +B](f: I => B) extends VwEvaluator[Any, I, B] {

  def apply(v1: Any, v2: I): B = f(v2)

}

final case class ContextualEvaluator[-A, -I, C, +B](ctx: A => C, f: (C, I) => B) extends VwEvaluator[A, I, B] {

  def apply(v1: A, v2: I): B = f(ctx(v1), v2)

}
