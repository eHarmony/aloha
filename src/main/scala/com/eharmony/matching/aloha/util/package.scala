package com.eharmony.matching.aloha

import scala.util.Try
import scalaz.Monad

package object util {

    /** A scalaz monad instance for ''scala.lang.Try''.
      */
    implicit object TryScalazMonad extends Monad[Try] {
        def point[A](a: => A) = Try(a)
        def bind[A, B](fa: Try[A])(f: (A) => Try[B]) = fa.flatMap(f)
    }
}
