package com.eharmony.aloha.semantics.compiled.plugin.schemabased.codegen

object MapType extends Enumeration {
  type MapType = Value
  val MAP = Value("map")
  val FLAT_MAP = Value("flatMap")
  val NONE = Value
}
