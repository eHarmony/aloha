package com.eharmony.aloha.models

import java.io.Closeable

import com.eharmony.aloha.id.{Identifiable, ModelIdentity}

/**
  * Created by ryan on 1/11/17.
  */
trait Model[-A, +B] extends (A => B)
                       with Identifiable[ModelIdentity]
                       with Closeable
