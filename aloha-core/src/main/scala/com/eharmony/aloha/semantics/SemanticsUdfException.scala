package com.eharmony.aloha.semantics

import scala.util.Try
import com.eharmony.aloha.AlohaException

/** An exception used to denote that an error occurred while applying a user-defined function on the input data.  This
  * was not a problem with the model but rather the code that the model called.  This is differentiated because the
  * code may have been defined outside of the Aloha libraries.
  *
  * The instantiation of this class should not throw exceptions.
  *
  * @param specification a specification for the feature that produced an error.
  * @param accessorOutput output of the accessor functions for the feature that produced an error.
  * @param accessorsMissingOutput names of accessor functions with missing data in the feature that produced an error.
  * @param accessorsInErr names of accessor functions that produced errors in the feature that produced an error.
  * @param cause the actual exception denoting the cause of the problem in the feature.
  * @param input input to the feature that caused a problem.
  *              ('''Note''': this parameter was intentionally chosen to go last because of a possible verbose toString output)
  */
case class SemanticsUdfException[+A](
        specification: String,
        accessorOutput: Map[String, Try[Any]],
        accessorsMissingOutput: List[String],
        accessorsInErr: List[String],
        cause: Throwable,
        input: A)
    extends AlohaException(SemanticsUdfException.getMessage(specification, accessorOutput, accessorsMissingOutput, accessorsInErr, input), cause)

private object SemanticsUdfException {

    /** This method guards against throwing exceptions.
      * @param specification a specification for the feature that produced an error.
      * @param accessorOutput output of the accessor functions for the feature that produced an error.
      * @param accessorsMissingOutput names of accessor functions with missing data in the feature that produced an error.
      * @param accessorsInErr names of accessor functions that produced errors in the feature that produced an error.
      * @param input input to the feature that caused a problem.
      * @param inputMaxSize the maximum length of the input's toString to retain.
      * @tparam A type of input
      * @return
      */
    def getMessage[A](
            specification: String,
            accessorOutput: Map[String, Try[Any]],
            accessorsMissingOutput: List[String],
            accessorsInErr: List[String],
            input: A,
            inputMaxSize: Int = 1000): String = {

        // Use these to avoid any possible NPEs.
        val out = Option(accessorOutput) getOrElse Map.empty
        val missing = Option(accessorsMissingOutput) getOrElse Nil
        val inErr = Option(accessorsInErr) getOrElse Nil
        val spec = Option(specification) getOrElse "specification not provided"

        val bad = Set((missing ++ inErr):_*)
        val errSet = Set(inErr:_*)
        val ok = out.collect { case (k, v) if !bad.contains(k) && v.isSuccess => (k, v) }
        val errs = out.collect { case (k, v) if errSet.contains(k) && v.isFailure => (k, v.recover{ case e: Exception => e.toString }.get.toString) }

        // Make sure to be careful about protecting against errors during toString of the input object.
        // This includes the possibility of null input, toString throwing an exception, and the resulting exception's
        // toString throwing an exception.
        val inputStr = Option(input).map { in =>
                           Try { in.toString }.
                           recover { case e => {
                               val suffix = Try { e.getMessage }.map{m => s": $m"}.recover{ case e1 => "" }.get
                               s"input toString threw ${e.getClass.getCanonicalName}$suffix"
                           }}.
                           get
                       }.orElse(Option("no input provided")).
                         take(inputMaxSize)

        val eStr = if (errs.nonEmpty) errs.map { case (k, v) => s"$k: $v" }.mkString("\n\tAccessors with errors:\n\t\t", ",\n\t\t", "") else ""
        val mStr = if (out.nonEmpty) out.mkString("\n\tAccessors with missing data: ", ", ", "") else ""
        val okStr = if (ok.nonEmpty) ok.map { case (k, v) => s"$k: $v" }.mkString("\n\tValid accessor values:\n\t\t", ",\n\t\t", "") else ""
        val msg = s"""specification "$spec" resulted in error.${eStr}${mStr}$okStr\n\tinput: $inputStr"""
        msg
    }
}
