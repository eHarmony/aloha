package com.eharmony.matching.aloha.semantics.compiled.plugin.csv

import com.eharmony.matching.aloha.semantics.compiled.{RequiredAccessorCode, OptionalAccessorCode, VariableAccessorCode, CompiledSemanticsPlugin}
import com.eharmony.matching.aloha.reflect.RefInfo

import scala.annotation.varargs

/**
  * Create a plugin that can generate code for TSV lines.
  * @param colNamesToTypes a mapping from field name to field type.
  */
case class CompiledSemanticsCsvPlugin(colNamesToTypes: Map[String, CsvTypes.CsvType] = Map.empty) extends CompiledSemanticsPlugin[CsvLine] {

   /**
     * @return a [[scala.reflect.runtime.universe.TypeTag]] for input type A.
     */
    val refInfoA = RefInfo[CsvLine]

    /** Generate the code necessary to compile into a function.
      * @param spec a string-based specification of the function.
      * @return
      */
    def accessorFunctionCode(spec: String): Either[Seq[String], VariableAccessorCode] = {
        val code = colNamesToTypes.get(spec).map {
            case t if t.isRequired => Right(RequiredAccessorCode(Seq(s"""(_:$inputTypeString).${t.toString}("${escape(spec)}")""")))
            case t => Right(OptionalAccessorCode(Seq(s"""(_:$inputTypeString).${t.toString}("${escape(spec)}")""")))
        }.getOrElse {
            Left(Seq[String](s"Couldn't produce code for specification: '$spec'."))
        }

        code
    }

    private[this] def escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
}

object CompiledSemanticsCsvPlugin {
    @varargs def apply(colNamesToTypes: (String, CsvTypes.CsvType)*): CompiledSemanticsCsvPlugin =
        CompiledSemanticsCsvPlugin(colNamesToTypes.toMap)
}
