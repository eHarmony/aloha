package com.eharmony.aloha.semantics

import com.eharmony.aloha.reflect.RefInfo

import scala.language.higherKinds

/**
  * A version of [[Semantics]] that can morph into other instance of the same structure but
  * with a different type parameter.
  * @tparam M a type constructor the gives
  * @tparam A
  * @author deaktator
  */
// TODO: Finish documentation...
trait MorphableSemantics[M[_] <: Semantics[_], A] { self: Semantics[A] =>

  /**
    * Get `this` instance as a `Semantics[A]`.
    *
    * This could be implemented fully here because the self-type allows it, but it is left for
    * the derived classes because they can return a more specific type.  For instance:
    *
    * {{{
    * case class X[A]() extends Semantics[A] with MorphableSemantics[X, A] {
    *   def semantics: X[A] = this
    *   def morph[B: RefInfo]: Option[X[B]] = Some(X[B]())
    * }
    * }}}
    *
    * can be used generically, in which case a value of type `Semantics[A]` is returned in cases
    * like the following:
    *
    * {{{
    * def generic[M[_] <: Sem[_], A](m: Morphable[M, A]): Sem[A] = m.semantics
    * }}}
    *
    * In cases where the concrete type is known, a more specific type can be return, e.g.:
    *
    * {{{
    * def specific[A](x: X[A]): X[A] = x.semantics
    * }}}
    * @return `this`, cast as `Semantics[A]`.
    */
  def semantics: Semantics[A]

  /**
    * Attempt to morph this [[MorphableSemantics]] into a new one with the same structure.
    *
    * The new [[MorphableSemantics]] will have the same structure (as dictated by the `M`
    * type parameter) but with a different second parameter.  This new instance must also be
    * a `Semantics[B]` because of the type bound on `M` and the self-type of
    * [[MorphableSemantics]].  It can be the case that a specific implementation of
    * [[MorphableSemantics]] doesn't allow for all `B`, so it can return a `None`.
    * @tparam B input type for the new [[MorphableSemantics]] instance that might be created.
    *           A [[MorphableSemantics]] instance may choose not allow morphing to all `B`.
    *           In that case, a `None` will be returned.
    * @return
    */
  def morph[B: RefInfo]: Option[MorphableSemantics[M, B]]
}
