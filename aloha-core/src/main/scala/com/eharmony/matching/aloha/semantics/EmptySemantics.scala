package com.eharmony.matching.aloha.semantics

import com.eharmony.matching.aloha.reflect.RefInfo

object EmptySemantics {
    def apply[A: RefInfo]: Semantics[A] = new Semantics[A] {
        def refInfoA = RefInfo[A]
        def close() {}
        def accessorFunctionNames = ???
        def createFunction[B: RefInfo](codeSpec: String, default: Option[B]) = ???
    }
}
