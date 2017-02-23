package com.eharmony.aloha.semantics.compiled.plugin.proto.codegen

object MapType extends Enumeration {
    type MapType = Value
    val MAP = Value("map")
    val FLAT_MAP = Value("flatMap")
    val NONE = Value
}
