package com.eharmony.aloha.algebra

import scalaz.Monad
import scala.util.Try

/** '''NOTE''': This package should be called '''try''' but this is a bad idea because it is a reserved word in Scala
  * and Java.  This probably could have been back-ticked but use plural instead to avoid obvious problems.
  */
package object tries {

    /** A scalaz monad type class instance for ''scala.lang.Try''.
      */
    implicit object TryScalazMonad extends Monad[Try] {
        def point[A](a: => A) = Try(a)
        def bind[A, B](fa: Try[A])(f: (A) => Try[B]) = fa.flatMap(f)
    }
}
