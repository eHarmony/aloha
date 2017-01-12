package com.eharmony.aloha.score.audit.take5

/**
  * Created by ryan on 1/11/17.
  */
trait Model[-A, +B] extends (A => B)
