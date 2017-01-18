package com.eharmony.aloha.score.audit.take5

/**
  * Created by ryan on 1/18/17.
  */
trait Submodel[+N, -A, +B] extends Model[A, B] {
  def subValue(a: A): (B, Option[N])
}
