package com.eharmony.aloha.semantics

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.func.{GenFunc0, GenFunc, GenAggFunc}

/** A semantics that will produces functions that always throw ''exception'' when called.
  *
  * @param exception  The exception to throw by the function produced by this semantics
  * @param evidence$1 reflection info about the input type
  * @tparam A the input type of the functions produced by this semantics.
  */
case class FunctionWithErrorProducingSemantics[A: RefInfo](exception: Exception) extends Semantics[A] {

    /**
      * @return a [[com.eharmony.aloha.reflect.RefInfo]] for input type A.
      */
    def refInfoA: RefInfo[A] = RefInfo[A]

    /** Returns the string representations of all of the data "variables" used by functions created from this Semantics
      * object.
      * @return
      */
    def accessorFunctionNames: Seq[String] = ???

    /** Create a function from A to B that always throws exception that was passed into the constructor.
      * @param codeSpec unused
      * @param default unused
      * @tparam B The return type of the function.
      * @return
      */
    def createFunction[B: RefInfo](codeSpec: String, default: Option[B] = None): Either[Seq[String], GenAggFunc[A, B]] = {
        val gaf: GenFunc0[A, Nothing] = GenFunc.f0(codeSpec, (_: A) => throw exception)
        val r = Right(gaf)
        r
    }

    def close() {}
}
