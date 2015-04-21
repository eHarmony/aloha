package com.eharmony.matching.featureSpecExtractor.util

import com.google.common.{base => guava}

object FunctionConversions {
    implicit def scala2guava[A, B](f: A => B): guava.Function[A, B] =
        new guava.Function[A, B] {
            def apply(a: A) = f(a)
        }

    implicit def guava2scala[A, B](f: guava.Function[A, B]): (A => B) = (a: A) => f(a)
}
