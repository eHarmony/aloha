package com.eharmony.aloha.semantics

import java.io.Closeable
import scala.annotation.implicitNotFound
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.reflect.RefInfo

@implicitNotFound(msg = "Cannot find Semantics for type ${A}.")
trait Semantics[A] extends Closeable {

    /**
      * @return a [[com.eharmony.aloha.reflect.RefInfo]] for input type A.
      */
    def refInfoA: RefInfo[A]

    /** Returns the string representations of all of the data "variables" used by functions created from this Semantics
      * object.
      * @return
      */
    def accessorFunctionNames: Seq[String]

    /** Create a function from A to B.
      * @param codeSpec specification for a function to be produced by this semantics.
      * @param default a default value in the case that the function would produce an optional type.
      * @tparam B The return type of the function.
      * @return
      */
    def createFunction[B: RefInfo](codeSpec: String, default: Option[B] = None): Either[Seq[String], GenAggFunc[A, B]]
}
