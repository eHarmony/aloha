package com.eharmony.aloha.score.audit.take5

/**
  * Created by ryan on 1/18/17.
  */
trait SubmodelBase[U, N, -A, +B <: U] extends Submodel[N, A, B] {
  final def apply(a: A): B = subvalue(a).audited
}
