package com.eharmony.aloha.score.audit.take3

/**
  * Created by ryan on 12/16/16.
  */
trait Model[-A, +B] extends (A => B)
