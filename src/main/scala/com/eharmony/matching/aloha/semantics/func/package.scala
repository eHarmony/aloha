package com.eharmony.matching.aloha.semantics.func

import scala.util.Try
import com.eharmony.matching.aloha.semantics.SemanticsUdfException

/** An extractor function containing additional information about itself.
  * @param descriptor A descriptor of the data being extracted from data input of type A
  * @param function The actual function used to extract the data from type A.
  * @param code The (optional) code implementation of the function
  * @tparam A data type from which we would like to extract a value of type B
  * @tparam B the type of data we want to extract from type A.
  */
case class GeneratedAccessor[-A, +B](descriptor: String, function: A => B, code: Option[String] = None) extends (A => B) {
    @inline def apply(a: A): B = function(a)
    override def toString() = "GeneratedAccessor(" + descriptor + (code map (", code: " + _) getOrElse "") + ")"
}

/** A trait for generated functions that aggregate the values of other functions.
  * NOTE: It is important that subclasses place the aggregated function first.  This allows the accessors
  *       function to work properly.  We seal this trait because the cast is dangerous if someone else improperly
  *       extends this trait.
  * @tparam A input type of the function
  * @tparam B output type of the function
  */
sealed trait GenAggFunc[-A, +B] extends (A => B) { self: Product =>
    /** The specification of the function.
      */
    val specification: String

    /** Get the accessors contained in the Function.
      * @return
      */
    def accessors: List[GeneratedAccessor[A, _]] = productIterator.drop(2).asInstanceOf[Iterator[GeneratedAccessor[A, _]]].toList

    /** Produce a list of results by applying each accessor on the input.
      * @param a the input
      * @return a map from each accessor's descriptor to the value produced by the accessor.
      */
//    def accessorOutput(a: A): Map[String, Any] = accessors.map(acc => (acc.descriptor, acc(a))).toMap

    def accessorOutput(a: A): Map[String, Try[Any]] = accessors.map(acc => (acc.descriptor, Try{acc(a)})).toMap

    /** Like Function1.andThen except it returns a GenAggFunc.
      * @param g a function to execute after this
      * @tparam C output type of the resulting function.
      * @return
      */
    def andThenGenAggFunc[C](g: B => C): GenAggFunc[A, C]

    /** Produce a list of accessor descriptors where the accessor results in missing data.  This is determined applying
      * accessorOutput and collecting keys whose values are None or Left(_).
      * @param a input on which we are trying to report accessors with missing outputs.
      * @return
      */
    def accessorOutputMissing(a: A): List[String] = accessorsWithMissing(accessorOutput(a))

    def accessorOutputWithError(a: A): List[String] = accessorsInErr(accessorOutput(a))

    /** Produce a list of accessor descriptors where the accessor results in missing data.  This is determined applying
      * accessorOutput and collecting keys whose values are None or Left(_).
      * @param ao input on which we are trying to report accessors with missing outputs.
      * @return
      */
    protected[this] final def accessorsWithMissing(ao: Map[String, Try[Any]]): List[String] = {
        ao.flatMap {
            case(k, v) if v.isSuccess => v.get match {
                case None => Option(k)
                case Left(_) => Option(k)
                case _ => None
            }
            case _ => None
        }.toList
    }

    /** Accessor names for accessors with errors.
      * @param ao output of accessorOutput applied to an input.
      * @return
      */
    protected[this] final def accessorsInErr(ao: Map[String, Try[Any]]): List[String] =
        ao.collect { case (k, v) if v.isFailure => k }.toList

    override def toString() = accessors.map(a => "${" + a.descriptor + "}").mkString("GenAggFunc((", ", ", ") => ") + specification + ")"
}

/** Provides a series of functions that create instances of GenFuncN.  These are preferable to calling the class
  * constructors directly because we can completely avoid specifying typing information do to the multiple argument
  * lists in the functions.  This helps especially when generating code via multi-stage programming.
  */
object GenFunc {
    val maxArity = 10

    /** Create a function that doesn't compute any intermediate values from A while producing a value of type B.
      * @param specification string specification of the function.
      * @param f a function taking no variables and returns a value of type C
      * @tparam A input type to the resulting function
      * @tparam B output type of the resulting function
      * @return a function F = f(x).  Essentially a delegate that delegates to f.  This mainly means that the
      *         definer of f needs to either make f invariant to an input of A or needs to have inside knowledge
      *         of A.
      */
    def f0[A, B](specification: String, f: A => B) = GenFunc0(specification, f)

    /** Create a function in one variable that takes an instance of A and returns a value of type C.  Note that
      * this function like the other functions in GenFunc are provided to assist type resolution.  When generating
      * code, it's really nice not to have to specify the type of the variables because we can just let the type
      * resolution mechanism in Scala take care of it.
      * {{{
      * // Get the value associated with the "one" key in the map and add 1 to it.
      * val f1 =
      *   GenFunc.f1(
      *     GeneratedAccessor(
      *       "one",                                      // The descriptor of the variable.
      *       (_:Map[String, Int]) get "one",             // The actual implementation.
      *       Some("""(_:Map[String, Int]) get "one"""")  // Optional String description of extractor.
      *     )
      *   )(
      *     "${one} + 1",    // Description of function.
      *     _.map(_ + 1)     // No typing information necessary.
      *   )
      *
      *   f1(Map("one" -> 1))               // Some(2)
      *   f1.accessorOutputMissing(Map())   // List("one")  The list of missing fields (variables) required by f1.
      * }}}
      * @param f1 A function that pulls a variables value out of the input type.
      * @param specification string specification of the function.
      * @param f a function taking one variable (the value returned by f1) that returns type C
      * @tparam A input type to the resulting function
      * @tparam B1 the type of the variable used in the function
      * @tparam C output type of the resulting function
      * @return a function F = f(f1(x)) in one variable that extracts x's value with accessor function f1.
      */
    def f1[A, B1, C](f1: GeneratedAccessor[A, B1])(specification: String, f: B1 => C) = GenFunc1(specification, f, f1)
    def f2[A, B1, B2, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2])(specification: String, f: (B1, B2) => C) = GenFunc2(specification, f, f1, f2)
    def f3[A, B1, B2, B3, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3])(specification: String, f: (B1, B2, B3) => C) = GenFunc3(specification, f, f1, f2, f3)
    def f4[A, B1, B2, B3, B4, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4])(specification: String, f: (B1, B2, B3, B4) => C) = GenFunc4(specification, f, f1, f2, f3, f4)
    def f5[A, B1, B2, B3, B4, B5, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5])(specification: String, f: (B1, B2, B3, B4, B5) => C) = GenFunc5(specification, f, f1, f2, f3, f4, f5)
    def f6[A, B1, B2, B3, B4, B5, B6, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6])(specification: String, f: (B1, B2, B3, B4, B5, B6) => C) = GenFunc6(specification, f, f1, f2, f3, f4, f5, f6)
    def f7[A, B1, B2, B3, B4, B5, B6, B7, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7])(specification: String, f: (B1, B2, B3, B4, B5, B6, B7) => C) = GenFunc7(specification, f, f1, f2, f3, f4, f5, f6, f7)
    def f8[A, B1, B2, B3, B4, B5, B6, B7, B8, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7], f8: GeneratedAccessor[A, B8])(specification: String, f: (B1, B2, B3, B4, B5, B6, B7, B8) => C) = GenFunc8(specification, f, f1, f2, f3, f4, f5, f6, f7, f8)
    def f9[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7], f8: GeneratedAccessor[A, B8], f9: GeneratedAccessor[A, B9])(specification: String, f: (B1, B2, B3, B4, B5, B6, B7, B8, B9) => C) = GenFunc9(specification, f, f1, f2, f3, f4, f5, f6, f7, f8, f9)
    def f10[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, C](f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7], f8: GeneratedAccessor[A, B8], f9: GeneratedAccessor[A, B9], f10: GeneratedAccessor[A, B10])(specification: String, f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10) => C) = GenFunc10(specification, f, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10)
}

/** A wrapper around an unsafe GenAggFunc that will catch exceptions and rethrow
  * [[com.eharmony.matching.aloha.semantics.SemanticsUdfException]] with the appropriate information filled in.
  * @param unsafe an unsafe GenAggFunc that can throw exceptions.
  * @tparam A input type of the function
  * @tparam B output type of the function
  */
case class EnrichedErrorGenAggFunc[-A, +B](unsafe: GenAggFunc[A, B]) extends GenAggFunc[A, B] {
    override def accessors = unsafe.accessors
    val specification = unsafe.specification

    /** Like Function1.andThen except it returns a GenAggFunc.  This calls the unsafe GenAggFunc's andThenGenAggFunc
      * function and then wraps the result in a RethrowingGenAggFunc.
      *
      * @param g a function to execute after this
      * @tparam C output type of the resulting function.
      * @return
      */
    def andThenGenAggFunc[C](g: B => C) = EnrichedErrorGenAggFunc(unsafe.andThenGenAggFunc(g))

    /** Apply the unsafe GenAggFunc.  If it results in a SemanticsUdfException, rethrow.  If it results in any other
      * type of Exception, gather data and create a SemanticsUdfException and throw it.
      * @param a function input
      * @return
      */
    @throws[SemanticsUdfException[A]]("when the underlying unsafe GenAggFunc throws an exception.")
    @inline def apply(a: A) =
        try {
            unsafe(a)
        }
        catch {
            case e: SemanticsUdfException[A] => throw e
            case e: Exception =>
                val ao = accessorOutput(a)
                val missing = accessorsWithMissing(ao)
                val err = accessorsInErr(ao)
                val ex = SemanticsUdfException(specification, ao, missing, err, e, a)
                throw ex
        }
}


case class OptionalFunc[-A, +B](function: GenAggFunc[A, Option[B]], default: B) extends GenAggFunc[A, B] {
    @inline def apply(a: A) = function(a) getOrElse default
    override def accessors = function.accessors
    val specification = function.specification
    def andThenGenAggFunc[C](g: B => C) = function andThenGenAggFunc ((b: Option[B]) => g(b getOrElse default))
    override def toString() = s"OptionalFunc(${function.toString()}, $default)"
}

case class GenFunc0[-A, +B](specification: String, f: A => B) extends GenAggFunc[A, B] {
    @inline def apply(a: A) = f(a)
    def andThenGenAggFunc[C](g: B => C) = copy(f = f.andThen(g))
}

case class GenFunc1[-A, B1, +C](specification: String, f: B1 => C, f1: GeneratedAccessor[A, B1]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = f.andThen(g))
}

case class GenFunc2[-A, B1, B2, +C](specification: String, f: (B1, B2) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2) => g(f(b1, b2)))
}

case class GenFunc3[-A, B1, B2, B3, +C](specification: String, f: (B1, B2, B3) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3) => g(f(b1, b2, b3)))
}

case class GenFunc4[-A, B1, B2, B3, B4, +C](specification: String, f: (B1, B2, B3, B4) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a), f4(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3, b4: B4) => g(f(b1, b2, b3, b4)))
}

case class GenFunc5[-A, B1, B2, B3, B4, B5, +C](specification: String, f: (B1, B2, B3, B4, B5) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a), f4(a), f5(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3, b4: B4, b5: B5) => g(f(b1, b2, b3, b4, b5)))
}

case class GenFunc6[-A, B1, B2, B3, B4, B5, B6, +C](specification: String, f: (B1, B2, B3, B4, B5, B6) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a), f4(a), f5(a), f6(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3, b4: B4, b5: B5, b6: B6) => g(f(b1, b2, b3, b4, b5, b6)))
}

case class GenFunc7[-A, B1, B2, B3, B4, B5, B6, B7, +C](specification: String, f: (B1, B2, B3, B4, B5, B6, B7) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a), f4(a), f5(a), f6(a), f7(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3, b4: B4, b5: B5, b6: B6, b7: B7) => g(f(b1, b2, b3, b4, b5, b6, b7)))
}

case class GenFunc8[-A, B1, B2, B3, B4, B5, B6, B7, B8, +C](specification: String, f: (B1, B2, B3, B4, B5, B6, B7, B8) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7], f8: GeneratedAccessor[A, B8]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a), f4(a), f5(a), f6(a), f7(a), f8(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3, b4: B4, b5: B5, b6: B6, b7: B7, b8: B8) => g(f(b1, b2, b3, b4, b5, b6, b7, b8)))
}

case class GenFunc9[-A, B1, B2, B3, B4, B5, B6, B7, B8, B9, +C](specification: String, f: (B1, B2, B3, B4, B5, B6, B7, B8, B9) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7], f8: GeneratedAccessor[A, B8], f9: GeneratedAccessor[A, B9]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a), f4(a), f5(a), f6(a), f7(a), f8(a), f9(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3, b4: B4, b5: B5, b6: B6, b7: B7, b8: B8, b9: B9) => g(f(b1, b2, b3, b4, b5, b6, b7, b8, b9)))
}

case class GenFunc10[-A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, +C](specification: String, f: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10) => C, f1: GeneratedAccessor[A, B1], f2: GeneratedAccessor[A, B2], f3: GeneratedAccessor[A, B3], f4: GeneratedAccessor[A, B4], f5: GeneratedAccessor[A, B5], f6: GeneratedAccessor[A, B6], f7: GeneratedAccessor[A, B7], f8: GeneratedAccessor[A, B8], f9: GeneratedAccessor[A, B9], f10: GeneratedAccessor[A, B10]) extends GenAggFunc[A, C] {
    @inline def apply(a: A) = f(f1(a), f2(a), f3(a), f4(a), f5(a), f6(a), f7(a), f8(a), f9(a), f10(a))
    def andThenGenAggFunc[D](g: C => D) = copy(f = (b1: B1, b2: B2, b3: B3, b4: B4, b5: B5, b6: B6, b7: B7, b8: B8, b9: B9, b10: B10) => g(f(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10)))
}
