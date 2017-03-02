package com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema

import com.eharmony.aloha.semantics.compiled.plugin.schemabased.schema.Schema.FieldRetrievalError

/**
  * Created by ryan.
  */
trait Schema {
  def field(name: String): Either[FieldRetrievalError, FieldDesc]
}

object Schema {
  case class FieldRetrievalError(error: String)
}
