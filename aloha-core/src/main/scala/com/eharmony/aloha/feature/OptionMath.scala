package com.eharmony.aloha.feature

object OptionMath extends OptionMath

/** Provides basic inline unary and binary mathematical operators for options of primitive types including.
  *
  * - Option[Byte]
  * - Option[Short]
  * - Option[Int]
  * - Option[Long]
  * - Option[Float]
  * - Option[Double]
  *
  * Additionally, operations are provided that can take any scala.math.Numeric type as long as an implicit of that
  * type is available at call time.
  *
  * This functionality assumes that the type arguments are the same as it relies on scala.math.Numeric.  When the types
  * are the same, everything works seamlessly.
  *
  * {{{
  * import com.eharmony.aloha.feature.OptionMath.Syntax._
  *
  * scala> Option(1) + None
  * res0: Option[Int] = None
  *
  * scala> None + Option(1)
  * res1: Option[Int] = None
  *
  * scala> Option(1) + Option(2)
  * res3: Option[Int] = Some(3)
  *
  * scala> Some(1L.toInt) + Option(2)
  * res4: Option[Int] = Some(3)
  *
  * scala> Option(1) < Option(2)
  * res5: Option[Boolean] = Some(true)
  *
  * scala> Option(1) >= Option(2)
  * res6: Option[Boolean] = Some(false)
  *
  * scala> Option(1) < None
  * res7: Option[Boolean] = None
  * }}}
  *
  * When the types for the function arguments don't line up, we get a compile time error.
  *
  * {{{
  * scala> Some(1) + Some(1.5)
  * <console>:12: error: type mismatch;
  * found   : Double(1.5)
  * required: Int
  *              Some(1) + Some(1.5)
  *                             ^
  * }}}
  *
  * Because inline operations require the use of implicit classes, inline syntax will incur an object creation
  * overhead that using OptMathOps will not.
  *
  * {{{
  * import com.eharmony.aloha.feature.OptionMath.Syntax._
  * import com.eharmony.aloha.feature.OptionMath.OptMathOps
  *
  * val a = Option(1.5)
  * val b = Option(0.5)
  *
  * val c = a / b                 // Additional object creation involved (slower, prettier)
  * val d = OptMathOps.div(a, b)  // Less object creation involved (faster, uglier)
  *
  * assert(c == d)
  * }}}
  *
  * Notice that the usual order of operations in the same.
  *
  * {{{
  * import com.eharmony.aloha.feature.OptionMath.Syntax._
  *
  * scala> Option(10 % 4 / 2) == Option(10) % Option(4) / Option(2)
  * res0: Boolean = true
  * }}}
  */
trait OptionMath {

    sealed protected[this] trait InlineSyntax[A, NumType <: Numeric[A]] {
        protected implicit val num: NumType
        val left: Option[A]

        @inline def abs() = OptMathOps.abs(left)
        @inline def unary_- = OptMathOps.negate(left)
        @inline def +(right: Option[A]) = OptMathOps.plus(left, right)
        @inline def -(right: Option[A]) = OptMathOps.minus(left, right)
        @inline def *(right: Option[A]) = OptMathOps.times(left, right)
        def /(right: Option[A]): Option[A]

        @inline def <(right: Option[A]) = OptMathOps.lt(left, right)
        @inline def <=(right: Option[A]) = OptMathOps.lteq(left, right)
        @inline def >(right: Option[A]) = OptMathOps.gt(left, right)
        @inline def >=(right: Option[A]) = OptMathOps.gteq(left, right)
    }

    sealed protected[this] trait InlineIntegralSyntax[A] extends InlineSyntax[A, Integral[A]] {
        @inline def /(right: Option[A]) = OptMathOps.quot(left, right)
        @inline def %(right: Option[A]) = OptMathOps.rem(left, right)
    }

    sealed protected[this] trait InlineFractionalSyntax[A] extends InlineSyntax[A, Fractional[A]] {
        @inline def /(right: Option[A]) = OptMathOps.div(left, right)
    }

    /** Should not use a wildcard import to include all functions in the object.  Instead, import the OptMathOps object.
      */
    object OptMathOps {
        @inline final def abs[A](a: Option[A])(implicit n: Numeric[A]) = a.map(n abs _)
        @inline final def negate[A](a: Option[A])(implicit n: Numeric[A]) = a.map(n negate _)
        @inline final def plus[A](left: Option[A], right: Option[A])(implicit n: Numeric[A]) = for (l <- left; r <- right) yield n.plus(l, r)
        @inline final def minus[A](left: Option[A], right: Option[A])(implicit n: Numeric[A]) = for (l <- left; r <- right) yield n.minus(l, r)
        @inline final def times[A](left: Option[A], right: Option[A])(implicit n: Numeric[A]) = for (l <- left; r <- right) yield n.times(l, r)

        @inline final def quot[A](left: Option[A], right: Option[A])(implicit n: Integral[A]) = for (l <- left; r <- right) yield n.quot(l, r)
        @inline final def rem[A](left: Option[A], right: Option[A])(implicit n: Integral[A]) = for (l <- left; r <- right) yield n.rem(l, r)
        @inline final def div[A](left: Option[A], right: Option[A])(implicit n: Fractional[A]) = for (l <- left; r <- right) yield n.div(l, r)

        @inline final def lt[A](left: Option[A], right: Option[A])(implicit n: Numeric[A]) = for (l <- left; r <- right) yield n.lt(l, r)
        @inline final def lteq[A](left: Option[A], right: Option[A])(implicit n: Numeric[A]) = for (l <- left; r <- right) yield n.lteq(l, r)
        @inline final def gt[A](left: Option[A], right: Option[A])(implicit n: Numeric[A]) = for (l <- left; r <- right) yield n.gt(l, r)
        @inline final def gteq[A](left: Option[A], right: Option[A])(implicit n: Numeric[A]) = for (l <- left; r <- right) yield n.gteq(l, r)
    }

    object Syntax {
        implicit class OptionByteMathSyntax(val left: Option[Byte])(implicit protected val num: Integral[Byte]) extends InlineIntegralSyntax[Byte]
        implicit class OptionShortMathSyntax(val left: Option[Short])(implicit protected val num: Integral[Short]) extends InlineIntegralSyntax[Short]
        implicit class OptionIntMathSyntax(val left: Option[Int])(implicit protected val num: Integral[Int]) extends InlineIntegralSyntax[Int]
        implicit class OptionLongMathSyntax(val left: Option[Long])(implicit protected val num: Integral[Long]) extends InlineIntegralSyntax[Long]
        implicit class OptionFloatMathSyntax(val left: Option[Float])(implicit protected val num: Fractional[Float]) extends InlineFractionalSyntax[Float]
        implicit class OptionDoubleMathSyntax(val left: Option[Double])(implicit protected val num: Fractional[Double]) extends InlineFractionalSyntax[Double]
    }
}
