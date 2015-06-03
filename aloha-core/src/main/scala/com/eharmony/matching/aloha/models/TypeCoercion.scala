package com.eharmony.matching.aloha.models

import java.{lang => jl}

import com.eharmony.matching.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.matching.aloha.util.Logging

import scala.math.ScalaNumericAnyConversions

/**
 * Provides over 200 coercions between types.  This works for the following matrix of types:
 *
 * <pre>
 *                                         TO
 *
 *             Bo  C   By  Sh  I   L   F   D   JBy JSh JI  JL  JF  JD  JC  JBo St
 *           +--------------------------------------------------------------------
 *       Bo  | I                                                           bB  tS
 *       C   |     I   A   A   A   A   A   A   A   A   A   A   A   A   A       tS
 *       By  |     A   I   A   A   A   A   A   A   A   A   A   A   A   A       tS
 *       Sh  |     A   A   I   A   A   A   A   A   A   A   A   A   A   A       tS
 *       I   |     A   A   A   I   A   A   A   A   A   A   A   A   A   A       tS
 *       L   |     A   A   A   A   I   A   A   A   A   A   A   A   A   A       tS
 *  F    F   |     A   A   A   A   A   I   A   A   A   A   A   A   A   A       tS
 *  R    D   |     A   A   A   A   A   A   I   A   A   A   A   A   A   A       tS
 *  O    JBy |         N   N   N   N   N   N   I   N   N   N   N   N   N       tS
 *  M    JSh |         N   N   N   N   N   N   N   I   N   N   N   N   N       tS
 *       JI  |         N   N   N   N   N   N   N   N   I   N   N   N   N       tS
 *       JL  |         N   N   N   N   N   N   N   N   N   I   N   N   N       tS
 *       JF  |         N   N   N   N   N   N   N   N   N   N   I   N   N       tS
 *       JD  |         N   N   N   N   N   N   N   N   N   N   N   I   N       tS
 *       JC  |     uC                                                  I       tS
 *       JBo | uB                                                          I   tS
 *       St  | fS      fS  fS  fS  fS  fS  fS  fS  fS  fS  fS  fS  fS      fS  I
 *           |
 * </pre>
 *
 * Where the label abbreviations are:
 *
 - '''''Bo''''':  ''scala.Boolean''
 - '''''C''''':   ''scala.Char''
 - '''''By''''':  ''scala.Byte''
 - '''''Sh''''':  ''scala.Short''
 - '''''I''''':   ''scala.Int''
 - '''''L''''':   ''scala.Long''
 - '''''F''''':   ''scala.Float''
 - '''''D''''':   ''scala.Double''
 - '''''JBy''''': ''java.lang.Byte''
 - '''''JSh''''': ''java.lang.Short''
 - '''''JI''''':  ''java.lang.Integer''
 - '''''JL''''':  ''java.lang.Long''
 - '''''JF''''':  ''java.lang.Float''
 - '''''JD''''':  ''java.lang.Character''
 - '''''JC''''':  ''java.lang.Double''
 - '''''JBo''''': ''java.lang.Boolean''
 - '''''St''''':  ''java.lang.String''
 *
 * and value abbreviations are:
 *
 - '''''A''''': ''conversion from scala.AnyVal''
 - '''''N''''': ''conversion from java.lang.Number''
 - '''''I''''': ''identity function''
 - '''''bB''''': ''boxing Boolean conversion''
 - '''''uB''''': ''unboxing Boolean conversion''
 - '''''uC''''': ''unboxing character conversion''
 - '''''tS''''': ''toString conversion''
 - '''''fS''''': ''from String conversion''
 *
 * The rationale behind not supplying conversion from String to Char is that an empty string is problematic.
 * One could call one of the following but they all throw exceptions:
 *
 * {{{
 * "".charAt(0) // java.lang.StringIndexOutOfBoundsException: String index out of range: 0
 * "".apply(0)  // java.lang.StringIndexOutOfBoundsException: String index out of range: 0
 * "".head      // java.util.NoSuchElementException: next on empty iterator
 * }}}
 *
 * Conversion to Boolean from Numbers and vice versa is not supported because there are a
 * number of ways to do this:
 *
 - Non-zero numbers are true, 0 is false.
 - One is true, all else is false
 *
 * @author R M Deak
 */
// TODO: Work on A = Option[C], B = Option[B] s.t. if Some[C => D], then produce Some[A => B]
trait TypeCoercion { self: Logging =>
    def coercion[A, B](implicit a: RefInfo[A], b: RefInfo[B]): Option[A => B] = {
        val coerceF =
            if (a == b) {
                val f: A => A = identity[A]
                debug(s"Using identity: ${RefInfoOps.toString[A]} == ${RefInfoOps.toString[B]}")
                Option(f.asInstanceOf[A => B])
            }
            else if (RefInfoOps.isSubType[A, Option[Any]] && RefInfoOps.isSubType[B, Option[Any]]) {
                coercion(RefInfoOps.typeParams[A].head, RefInfoOps.typeParams[B].head).asInstanceOf[Option[Any => Any]].map(f => (x: Option[Any]) => x.map(f)).asInstanceOf[Option[A => B]]
            }
            else if (!RefInfoOps.isSubType[A, Option[Any]] && RefInfoOps.isSubType[B, Option[Any]]) {
                coercion(a, RefInfoOps.typeParams[B].head).map(f => (x: A) => Option(f(x))).asInstanceOf[Option[A => B]]
            }
            else if (a == RefInfo.String) {
                debug(s"Using fromString: A=${RefInfoOps.toString[B]}")
                b match {
                    case RefInfo.Byte          => Option(((s: String) => s.toByte).asInstanceOf[A => B])
                    case RefInfo.Short         => Option(((s: String) => s.toShort).asInstanceOf[A => B])
                    case RefInfo.Int           => Option(((s: String) => s.toInt).asInstanceOf[A => B])
                    case RefInfo.Long          => Option(((s: String) => s.toLong).asInstanceOf[A => B])
                    case RefInfo.Float         => Option(((s: String) => s.toFloat).asInstanceOf[A => B])
                    case RefInfo.Double        => Option(((s: String) => s.toDouble).asInstanceOf[A => B])
                    case RefInfo.JavaByte      => Option(((s: String) => jl.Byte.valueOf(s)).asInstanceOf[A => B])
                    case RefInfo.JavaShort     => Option(((s: String) => jl.Short.valueOf(s)).asInstanceOf[A => B])
                    case RefInfo.JavaInteger   => Option(((s: String) => jl.Integer.valueOf(s)).asInstanceOf[A => B])
                    case RefInfo.JavaLong      => Option(((s: String) => jl.Long.valueOf(s)).asInstanceOf[A => B])
                    case RefInfo.JavaFloat     => Option(((s: String) => jl.Float.valueOf(s)).asInstanceOf[A => B])
                    case RefInfo.JavaDouble    => Option(((s: String) => jl.Double.valueOf(s)).asInstanceOf[A => B])
                    case _ => None
                }
            }
            else if (b == RefInfo.String) {
                debug(s"Using toString: B=${RefInfoOps.toString[B]}")
                Option(((a: A) => a.toString).asInstanceOf[A => B])
            }
            else if (RefInfo.Boolean == a && RefInfo.JavaBoolean == b) {
                debug(s"Using boxing boolean coercion: ${RefInfoOps.toString[A]} => ${RefInfoOps.toString[B]}")
                Option(((bool: Boolean) => jl.Boolean.valueOf(bool)).asInstanceOf[A => B])
            }
            else if (RefInfo.JavaBoolean == a && RefInfo.Boolean == b) {
                debug(s"Using unboxing boolean coercion: ${RefInfoOps.toString[A]} => ${RefInfoOps.toString[B]}")
                Option(((bool: jl.Boolean) => bool.booleanValue).asInstanceOf[A => B])
            }
            else if (RefInfo.JavaCharacter == a && RefInfo.Char == b) {
                debug(s"Using unboxing character coercion: ${RefInfoOps.toString[A]} => ${RefInfoOps.toString[B]}")
                Option(((c: jl.Character) => c.charValue).asInstanceOf[A => B])
            }
            else if (RefInfoOps.isSubType[A, jl.Number]) {
                val f = (a: A) => a.asInstanceOf[jl.Number]
                val c = b match {
                    case RefInfo.Byte          => Option(f.andThen(_.byteValue).asInstanceOf[A => B])
                    case RefInfo.Short         => Option(f.andThen(_.shortValue).asInstanceOf[A => B])
                    case RefInfo.Int           => Option(f.andThen(_.intValue).asInstanceOf[A => B])
                    case RefInfo.Long          => Option(f.andThen(_.longValue).asInstanceOf[A => B])
                    case RefInfo.Float         => Option(f.andThen(_.floatValue).asInstanceOf[A => B])
                    case RefInfo.Double        => Option(f.andThen(_.doubleValue).asInstanceOf[A => B])
                    case RefInfo.JavaByte      => Option(f.andThen(v => jl.Byte.valueOf(v.byteValue)).asInstanceOf[A => B])
                    case RefInfo.JavaShort     => Option(f.andThen(v => jl.Short.valueOf(v.shortValue)).asInstanceOf[A => B])
                    case RefInfo.JavaInteger   => Option(f.andThen(v => jl.Integer.valueOf(v.intValue)).asInstanceOf[A => B])
                    case RefInfo.JavaLong      => Option(f.andThen(v => jl.Long.valueOf(v.longValue)).asInstanceOf[A => B])
                    case RefInfo.JavaFloat     => Option(f.andThen(v => jl.Float.valueOf(v.floatValue)).asInstanceOf[A => B])
                    case RefInfo.JavaDouble    => Option(f.andThen(v => jl.Double.valueOf(v.doubleValue)).asInstanceOf[A => B])
                    case _ => None
                }
                c.foreach(_ => debug(s"Using function from java.lang.Number conversion: ${RefInfoOps.toString[A]} => ${RefInfoOps.toString[B]}."))
                c
            }
            else if (RefInfoOps.isSubType[A, AnyVal]) {
                val numConv = a match {
                    case RefInfo.Char   => Option(implicitly[Char => ScalaNumericAnyConversions].asInstanceOf[A => ScalaNumericAnyConversions])
                    case RefInfo.Byte   => Option(implicitly[Byte => ScalaNumericAnyConversions].asInstanceOf[A => ScalaNumericAnyConversions])
                    case RefInfo.Short  => Option(implicitly[Short => ScalaNumericAnyConversions].asInstanceOf[A => ScalaNumericAnyConversions])
                    case RefInfo.Int    => Option(implicitly[Int => ScalaNumericAnyConversions].asInstanceOf[A => ScalaNumericAnyConversions])
                    case RefInfo.Long   => Option(implicitly[Long => ScalaNumericAnyConversions].asInstanceOf[A => ScalaNumericAnyConversions])
                    case RefInfo.Float  => Option(implicitly[Float => ScalaNumericAnyConversions].asInstanceOf[A => ScalaNumericAnyConversions])
                    case RefInfo.Double => Option(implicitly[Double => ScalaNumericAnyConversions].asInstanceOf[A => ScalaNumericAnyConversions])
                    case _ => None
                }

                val c = numConv.flatMap(f => b match {
                    case RefInfo.Char          => Option(f.andThen(_.toChar).asInstanceOf[A => B])
                    case RefInfo.Byte          => Option(f.andThen(_.toByte).asInstanceOf[A => B])
                    case RefInfo.Short         => Option(f.andThen(_.toShort).asInstanceOf[A => B])
                    case RefInfo.Int           => Option(f.andThen(_.toInt).asInstanceOf[A => B])
                    case RefInfo.Long          => Option(f.andThen(_.toLong).asInstanceOf[A => B])
                    case RefInfo.Float         => Option(f.andThen(_.toFloat).asInstanceOf[A => B])
                    case RefInfo.Double        => Option(f.andThen(_.toDouble).asInstanceOf[A => B])
                    case RefInfo.JavaCharacter => Option(f.andThen(v => jl.Character.valueOf(v.toChar)).asInstanceOf[A => B])
                    case RefInfo.JavaByte      => Option(f.andThen(v => jl.Byte.valueOf(v.toByte)).asInstanceOf[A => B])
                    case RefInfo.JavaShort     => Option(f.andThen(v => jl.Short.valueOf(v.toShort)).asInstanceOf[A => B])
                    case RefInfo.JavaInteger   => Option(f.andThen(v => jl.Integer.valueOf(v.toInt)).asInstanceOf[A => B])
                    case RefInfo.JavaLong      => Option(f.andThen(v => jl.Long.valueOf(v.toLong)).asInstanceOf[A => B])
                    case RefInfo.JavaFloat     => Option(f.andThen(v => jl.Float.valueOf(v.toFloat)).asInstanceOf[A => B])
                    case RefInfo.JavaDouble    => Option(f.andThen(v => jl.Double.valueOf(v.toDouble)).asInstanceOf[A => B])
                    case _ => None
                })
                c.foreach(_ => debug(s"Using function from AnyVal conversion: ${RefInfoOps.toString[A]} => ${RefInfoOps.toString[B]}."))
                c
            }
            else None

        if (coerceF.isEmpty)
            warn("Couldn't find type coercion function: ${RefInfoOps.toString[A]} => ${RefInfoOps.toString[B]}.")

        coerceF
    }
}

object TypeCoercion extends TypeCoercion with Logging {
    def apply[A: RefInfo, B: RefInfo] = coercion[A, B]
}
