package com.eharmony.matching.aloha.semantics

import scala.util.Try
import com.eharmony.matching.aloha.AlohaException

/** An exception used to denote that an error occurred while applying a user-defined function on the input data.  This
  * was not a problem with the model but rather the code that the model called.  This is differentiated because the
  * code may have been defined outside of the Aloha libraries.
  * @param specification a specification for the feature that produced an error.
  * @param accessorOutput output of the accessor functions for the feature that produced an error.
  * @param accessorsMissingOutput names of accessor functions with missing data in the feature that produced an error.
  * @param accessorsinErr names of accessor functions that produced errors in the feature that produced an error.
  * @param cause the actual exception denoting the cause of the problem in the feature.
  * @param input input to the feature that caused a problem.
  *              ('''Note''': this parameter was intentionally chosen to go last because of a possible verbose toString output)
  */
case class SemanticsUdfException[+A](
        specification: String,
        accessorOutput: Map[String, Try[Any]],
        accessorsMissingOutput: List[String],
        accessorsinErr: List[String],
        cause: Throwable,
        input: A)
    extends AlohaException(SemanticsUdfException.getMessage(specification, accessorOutput, accessorsMissingOutput, accessorsinErr, input), cause)

private object SemanticsUdfException {
    def getMessage[A](
            specification: String,
            accessorOutput: Map[String, Try[Any]],
            accessorsMissingOutput: List[String],
            accessorsinErr: List[String],
            input: A): String = {

        val bad = Set((accessorsMissingOutput ++ accessorsinErr):_*)
        val errSet = Set(accessorsinErr:_*)
        val ok = accessorOutput.collect { case (k, v) if !bad.contains(k) && v.isSuccess => (k, v) }
        val errs = accessorOutput.collect { case (k, v) if errSet.contains(k) && v.isFailure => (k, v.recover{ case e: Exception => e.toString }.get.toString) }

        val eStr = if (errs.nonEmpty) errs.map { case (k, v) => s"$k: $v" }.mkString("\n\tAccessors with errors:\n\t\t", ",\n\t\t", "") else ""
        val mStr = if (accessorsMissingOutput.nonEmpty) accessorsMissingOutput.mkString("\n\tAccessors with missing data: ", ", ", "") else ""
        val okStr = if (ok.nonEmpty) ok.map { case (k, v) => s"$k: $v" }.mkString("\n\tValid accessor values:\n\t\t", ",\n\t\t", "") else ""
        val msg = s"""specification "$specification" resulted in error.${eStr}${mStr}$okStr\n\tinput: $input"""
        msg
    }
}
