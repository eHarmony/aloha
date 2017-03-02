package com.eharmony.aloha.semantics.compiled.plugin.schemabased.codegen

import com.eharmony.aloha.semantics.compiled.plugin.schemabased.accessor.FieldAccessor

/**
  * Created by ryan.
  */
trait AccessorCodeGenerator[A <: FieldAccessor] {
  def generateGet(a: A): String
}
