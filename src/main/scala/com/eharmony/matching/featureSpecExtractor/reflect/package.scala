package com.eharmony.matching.featureSpecExtractor

import scala.language.{existentials, implicitConversions}
import scala.util.Try

/**
 * This package acts as a place where an outside ETL project was pulled in to aloha-core.  The package is as is so
 * that internal eHarmony calling code doesn't need to change.  Since code here was originally a downstream project, it
 * included dependencies on ''aloha-core'', ''aloha-compiled-semantics'' and ''aloha-compiled-semantics-proto-plugin''.
 * because we don't want to pull in the latter two projects as dependencies, we will want to instantiate instances
 * present in those projects reflectively.  This isn't ideal but it will allow us to create specific types of semantics
 * without explicitly having access to their definitions at compile time.
 *
 * @author R M Deak
 */
package object reflect {

    /**
     * Representation of a param type for a reflective constructor call.
     */
    sealed trait ParamType

    /**
     * Upstream parameter types are '''known''' so we can use the Class object.
     * @param clazz a class object associate with a Constructor parameter.
     */
    final case class UpstreamParamType(clazz: Class[_]) extends ParamType


    /**
     * Downstream parameter types are '''unknown''' so we use a String representation and convert at runtime
     * to a Class.
     * @param className the name of the class in a constructor.
     */
    final case class DownstreamParamType(className: String) extends ParamType

    implicit def string2ParamType(s: String): ParamType = DownstreamParamType(s)
    implicit def class2ParamType[A](c: Class[A]): ParamType = UpstreamParamType(c)

    private[reflect] implicit class ParamListHelpers(val s: Seq[ParamType]) extends AnyVal {
        /**
         * Attempt to convert the downstream parameters to classes at runtime.  Errors are all or nothing
         * so we get a Try with either a sequence of Class objects or an exception.
         * @return
         */
        def sequenceOfClasses: Try[Seq[Class[_]]] = {

            // Need explicit typing for scalaz.
            val tries: Seq[Try[Class[_]]] = s.map {
                case DownstreamParamType(name) => Try { Class.forName(name) }
                case UpstreamParamType(clazz) => Try { clazz }
            }

            import scalaz.std.indexedSeq.indexedSeqInstance  // necessary
            import com.eharmony.matching.aloha.algebra.tries.TryScalazMonad
            import scalaz.syntax.traverse.ToTraverseOps
            tries.toIndexedSeq.sequence[Try, Class[_]]
        }

        /**
         * Get a string represenation of a list of parameter types.  Used in error reporting.
         * @return
         */
        def paramListString =
            s.map {
                case DownstreamParamType(name) => name
                case UpstreamParamType(clazz) => clazz.getCanonicalName
            }.mkString(", ")
    }
}
