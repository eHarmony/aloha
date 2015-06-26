package com.eharmony.aloha.feature

trait Intercept { self: DefaultPossessor =>

    /** Provides an intercept function that returns a sequence of one key-value pair:
      * {{{
      * assert(intercept() == Seq(("", 1.0)))
      * }}}
      * @return
      */
    @inline def intercept() = empty
}
