package com.eharmony.aloha.score.audit.take2

/**
  * Created by ryan on 12/15/16.
  */
trait Model[-A, +B] extends (A => B)
