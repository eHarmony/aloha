package com.eharmony.aloha.semantics

/** This is passed to the factory.
  * @tparam A
  */
trait SemanticsProvider[A] {
    /** Get a new instance of semantics.
      * @return
      */
    def semanticsInstance: Semantics[A]
}
