package com.eharmony.aloha.semantics

import java.util.concurrent.atomic.AtomicBoolean

/**
  * Provides support for testing that feature functions produced by semantics
  * do not call the feature function body when optional data is not present.
  *
  * This should probably be mixed into unit test companion objects.
  *
  * @author deaktator
  * @since 11/28/2017
  */
private[aloha] abstract class LazyInitSupport {
  private val NotCalled = false
  private val Called = true

  /**
    * Only to be modified by `wasCalled`.
    */
  private val indicator = new AtomicBoolean(NotCalled)

  /**
    * Attempt to set `indicator` to `true` assuming `indicator` wasn't already set.
    * `true` if indicator wasn't previously set; otherwise, `false`.
    */
  private lazy val wasCalled = indicator.compareAndSet(NotCalled, Called)

  /**
    * This function should be called in a feature function created by the
    * semantics to test that when `a` represents optional data that is not
    * present, the `lazy val` is not initialized.  For instance:
    *
    * {{{
    * val semantics: Semantics[Option[String]] = ???
    * val spec = "code(${missingVariable})"
    * val default = Some(None)
    * val f: GenAggFunc[A, Option[Boolean]] =
    *   semantics.createFunction[Option[Boolean]](spec, default).right.get
    * val y = f(None)
    * assertFalse(codeWasCalled)
    * }}}
    * @param a some value.
    * @return `true`, ''assuming'' `indicator` ''wasn't modified''.
    */
  def code(a: Any): Boolean = wasCalled

  /**
    * @return whether `code` was called.
    */
  def codeWasCalled: Boolean = indicator.get
}
