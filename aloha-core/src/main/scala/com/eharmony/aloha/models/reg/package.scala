package com.eharmony.aloha.models

package object reg {

  /**
   * Provides an extension method ''toKv'' to convert Options to Seq[(String, Double)].
   * This is used to coerce the value to the type that is used in regression models.
   * We don't do an implicit conversion method from Option[A] to Iterable[(String, Double)]
   * because it can negatively impact type inference.  So we make the users convert explicitly
   * via:
   *
   * {{{
   * val option: Option[Int] = Option(1)
   * val iterable = option.toKv
   * require(iterable == List(("", 1d)))
   * }}}
   * @param a an Option
   * @tparam A the type of Option.
   */
  final class OptToKv[A](val a: Option[A]) extends AnyVal {

    /**
     * Convert to Seq[(String, Double)].  If ''a'' is None, return Nil.  Otherwise, return
     * ''("", f(x)) :: Nil''.
     * @param f function to convert ''A'' to ''Double''.
     * @return a sequence
     */
    def toKv(implicit f: A => Double): Seq[(String, Double)] =
      a.fold(List.empty[(String, Double)])(x => ("", f(x)) :: Nil)
  }
}
