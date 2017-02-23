package com.eharmony.aloha.semantics.compiled.plugin.proto.codegen

import com.eharmony.aloha.semantics.compiled.plugin.proto.accessor.FieldAccessor

trait RequiredAccessorCodeGenerator[A <: FieldAccessor] {
    def generateGet(a: A): String
}
