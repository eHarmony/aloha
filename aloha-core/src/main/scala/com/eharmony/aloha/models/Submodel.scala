package com.eharmony.aloha.models

import java.io.Closeable

import com.eharmony.aloha.id.{Identifiable, ModelIdentity}

/**
  * Created by ryan on 1/18/17.
  */
trait Submodel[+N, -A, +B] extends Identifiable[ModelIdentity]
                              with Closeable {
  def subvalue(a: A): Subvalue[B, N]
}
