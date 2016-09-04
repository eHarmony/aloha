package com.eharmony.aloha.semantics

import com.eharmony.aloha.reflect.RefInfo

import scala.language.higherKinds

/**
  * A version of [[Semantics]] that can morph into other instances of the same structure but
  * with a different type parameter.
  *
  * An example:
  *
  * {{{
  * case class X[A]() extends Semantics[A] with MorphableSemantics[X, A] {
  *   def semantics: X[A] = this
  *   def morph[B: RefInfo]: Option[X[B]] = Some(X[B]())
  * }
  * }}}
  *
  * Notice for class `X`, that the `M` type constructor is `X` and the `A` is the same for
  * the second type parameter of [[MorphableSemantics]] as for the type parameter in [[Semantics]].
  *
  * This can be use like the following:
  *
  * {{{
  * import com.eharmony.aloha.reflect.RefInfo
  *
  * val xF = X[Float]
  * val riD = implicitly[RefInfo[Double]]
  * val xDo: Option[X[Double]] = xF.morph[Double]
  * val xDo1: Option[X[Double]] = xF.morph(riD)
  * }}}
  *
  * or use it generically:
  *
  * {{{
  * def morph[M[_] <: Semantics[_], A, B: RefInfo](m: MorphableSemantics[M, A]): Option[Semantics[B]] = {
  *   m.morph[B].map(ms => ms.semantics)
  * }
  *
  * val sDO: Option[Semantics[Double]] = morph(xF)
  * }}}
  *
  * @tparam M a type constructor providing the structure of the [[Semantics]].  This will most
  *           likely always be the concrete subclass of [[Semantics]] that implements
  *           [[MorphableSemantics]].
  * @tparam A the type parameter to which `M` is applied for this instance of [[Semantics]].
  *           By the self-type, also the type parameter for the [[Semantics]].
  * @author deaktator
  */
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
    *
    * '''Note''':The return type `this.type` could have been used, but that causes some issues
    *            later.  This doesn't have the problems and is easy for the implementer to add.
    *
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
    * [[MorphableSemantics]] doesn't allow for all `B`, in which case a `None` can be returned.
    * @param ri reflection information that may be necessary to determine whether to create
    *           the [[MorphableSemantics]] that was requested.
    * @tparam B input type for the new [[MorphableSemantics]] instance that might be created.
    *           A [[MorphableSemantics]] instance may choose not allow morphing to all `B`.
    *           In that case, a `None` will be returned.
    * @return
    */
  def morph[B](implicit ri: RefInfo[B]): Option[MorphableSemantics[M, B]]
}
