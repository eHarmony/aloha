package com.eharmony.matching.aloha.semantics.compiled

package object compiler {
    private[compiler] implicit class BooleanWrapper(val b: Boolean) extends AnyVal {
        def or(s: => String) = (if (b) Right(b) else Left(new IllegalArgumentException(s))).right
    }
}
