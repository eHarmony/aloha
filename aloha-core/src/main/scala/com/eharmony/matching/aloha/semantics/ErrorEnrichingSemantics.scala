package com.eharmony.matching.aloha.semantics

import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.semantics.func.{EnrichedErrorGenAggFunc, GenAggFunc}

/** User-defined functions used in feature specifications can cause models to throw undesirable exceptions.  This
  * semantics mixin can allow the the exception to be caught, reformulated as a
  * [[com.eharmony.matching.aloha.semantics.SemanticsUdfException]] and rethrown.  This has the distinct advantage of
  * providing the caller with the exact feature specification that failed along with the features that were present and
  * missing and the input data that caused the problem.
  *
  * ''Example usage'': Assume a we have an implementation UnsafeSemantics, implementing
  * [[com.eharmony.matching.aloha.semantics.Semantics]]\[Xyz\].  We can recast the errors by doing
  * {{{
  * // An object (note that because provideSemanticsUdfException = true by default, we don't need to specify it if
  * // we want this semantics to always enrich exceptions.
  * object SemanticsObjWithEnrichedErrors
  *     extends UnsafeSemantics
  *     with RethrowingSemantics[Xyz]
  *
  * // Here's a way to create a class to allow the caller to specify whether he wants
  * // [[com.eharmony.matching.aloha.semantics.SemanticsUdfException]]s to be provided.
  * case class SemanticsClsWithEnrichedErrors(override val provideSemanticsUdfException: Boolean = true)
  *     extends UnsafeSemantics
  *     with RethrowingSemantics[Xyz]
  * }}}
  * {{{
  * val featureSpecification = "[Some feature spec here ...]"
  * val xyz: Xyz = getXyz()
  * // Don't just call right.get in prod.
  * val f = SemanticsObjWithEnrichedErrors.createFunction[SomeOutputType](featureSpecification).right.get
  *
  * // If the following line throws an exception, it'll throw a SemanticsUdfException which provides additional
  * // information.
  * val r = f(xyz)
  * }}}
  * @tparam A the input type.
  * @author rdeak
  */
trait ErrorEnrichingSemantics[A] extends Semantics[A] {

    /** Only when this function returns true will the functions produced by this semantics be wrapped in a
      * [[com.eharmony.matching.aloha.semantics.func.EnrichedErrorGenAggFunc]].  The default for this function is true.
      * Therefore, when mixing in this trait override this function if we don't want to always wrap the generated
      * functions.
      * @return
      */
    protected[this] def provideSemanticsUdfException: Boolean = true

    /** Create a function from A to B.  If ''provideSemanticsUdfException'' is ''false'' or the generated function is
      * is [[com.eharmony.matching.aloha.semantics.func.EnrichedErrorGenAggFunc]] return the function; otherwise,
      * return the function wrapped in an [[com.eharmony.matching.aloha.semantics.func.EnrichedErrorGenAggFunc]].
      * [[com.eharmony.matching.aloha.semantics.func.GenAggFunc]] produced by the underlying semantics.
      * @param codeSpec specification for a function to be produced by this semantics.
      * @param default a default value in the case that the function would produce an optional type.
      * @tparam B The return type of the function.
      * @return
      */
    abstract override def createFunction[B: RefInfo](codeSpec: String, default: Option[B] = None): Either[Seq[String], GenAggFunc[A, B]] =
        super.createFunction[B](codeSpec, default).right.map {
            case f: EnrichedErrorGenAggFunc[A, B] => f
            case f: GenAggFunc[A, B] if provideSemanticsUdfException => EnrichedErrorGenAggFunc(f)
            case f => f
        }
}
