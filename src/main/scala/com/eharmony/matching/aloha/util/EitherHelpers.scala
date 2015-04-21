package com.eharmony.matching.aloha.util

import scalaz.{ Validation, ValidationNel, NonEmptyList }

trait EitherHelpers {

  /**
   * Either of Non-empty Seq (Like poor man's version of ValidationNel from scalaz)
   * @tparam X the type of success.
   */
  protected[this]type ENS[+A] = Either[Seq[String], A]

  protected[this] def toValidationNel[A](e: ENS[A]): ValidationNel[String, A] =
    Validation.fromEither(e.left.map(s => NonEmptyList(s.head, s.tail: _*)))

  protected[this] def fromValidationNel[A](v: ValidationNel[String, A]): ENS[A] =
    v.leftMap(l => l.head :: l.tail).toEither

  /**
   * Like l.map(f).sequence[({type L[+A] = Either[Seq[String], A]})#L, C ] in scalaz except that it short circuits
   * if it finds an error.  (There must be some better way to do this w/ scalaz).
   *
   * If we put a println("folding") at the top of the inner function h, we would get the following:
   * {{{
   * scala> mapSeq(Left(Seq("1")) +: (2 to 3).map(Right(_)))(identity)  // Only 1 "folding" instead of 3.
   * folding
   * res0: ENS[Seq[Int]] = Left(List(0))
   *
   * scala> mapSeq((1 to 3).map(Right(_)))(identity)
   * folding
   * folding
   * folding
   * res1: ENS[Seq[Int]] = Right(List(1, 2, 3))
   * }}}
   * @param l list of values to which f should be applied.
   * @param f function to map over l
   * @tparam A type of values in the input sequence in the first parameter list.
   * @tparam B type of values in the output sequence if successful.
   * @return
   */
  protected[this] def mapSeq[A, B](l: Seq[A])(f: A => ENS[B]): ENS[Seq[B]] = {
    def h(acc: ENS[List[B]], l: Seq[A], f: A => ENS[B]): ENS[Seq[B]] = {
      if (acc.isLeft || l.isEmpty) acc.right.map(_.reverse)
      else h(f(l.head).right.flatMap(s => acc.right.map(s :: _)), l.tail, f)
    }
    if (l.isEmpty) Right(Nil) else h(f(l.head).right.map(List(_)), l.tail, f)
  }
}
