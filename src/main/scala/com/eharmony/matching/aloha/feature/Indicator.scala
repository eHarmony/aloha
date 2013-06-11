package com.eharmony.matching.aloha.feature

trait Indicator { self: DefaultPossessor =>
    @inline def ind(a: AnyVal): Iterable[(String, Double)] = h(a)
    @inline def ind[A >: AnyVal with String](a: Option[A]): Iterable[(String, Double)] = a.map(h) getOrElse DefaultForMissingDataInReg
    @inline def ind(a: String): Iterable[(String, Double)] = h(a)

    /** Don't want to expose this directly.  Any is too broad.
      * @param a
      * @return
      */
    @inline private[this] def h(a: Any) = Iterable((s"=$a", 1.0))
}
