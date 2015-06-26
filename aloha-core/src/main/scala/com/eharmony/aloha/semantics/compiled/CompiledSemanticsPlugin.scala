package com.eharmony.aloha.semantics.compiled

import com.eharmony.aloha.reflect.{RefInfoOps, RefInfo}


/** A semantics object is responsible for parsing a variable accessor specification inside a dollar sign-curly brace
  * expression (that may be embedded in a larger expression).  For instance, in the variable accessor specification:
  * '''`\${a.b[0].c}`''', the semantics is responsible for transforming the string '''`a.b[0].c`''' into Scala code
  * for a function that pulls the data out of an object of type A.  The output type of the function need not
  * be stated explicitly because Scala's type inference mechanism will be used at the time the generated function
  * is compiled.
  * @tparam A the input type to a model
  */
trait CompiledSemanticsPlugin[A] {
    /**
      * @return a [[scala.reflect.runtime.universe.TypeTag]] for input type A.
      */
    def refInfoA: RefInfo[A]

    /** Generate the code necessary to compile into a function.
      * @param spec a string-based specification of the function.
      * @return
      */
    def accessorFunctionCode(spec: String): Either[Seq[String], VariableAccessorCode]

    /** Provides the string representation of the input type including any type parameters.  This utilizes the Scala
      * reflection capabilities of 2.10+.
      * @return
      */
    def inputTypeString = RefInfoOps toString refInfoA
}
