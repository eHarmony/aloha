package com.eharmony.aloha.models.h2o.compiler

import javax.tools.{Diagnostic, JavaFileObject}

import scala.io.Source


/**
 * Created by deak on 9/29/15.
 */
case class CompilationError(diagnostics: Iterable[Diagnostic[_ <: JavaFileObject]]) extends RuntimeException {
  override def getMessage: String = {
    val localMessage = diagnostics.map{d =>
      s"${d.getKind}: at ${d.getLineNumber}L, ${d.getColumnNumber}C: ${d.getMessage(null)}\n  line: ${getSourceLine(d)}"
    }.mkString("\n")
    super.getMessage + "\n" + localMessage
  }

  private[this] def getSourceLine[A <: JavaFileObject](diagnostic: Diagnostic[A]): String = {
    val source = Source.fromInputStream(diagnostic.getSource.openInputStream())
    val lineNum = diagnostic.getLineNumber.toInt

    try source.getLines.slice(lineNum - 1, lineNum).toSeq.headOption.getOrElse("")
    finally source.close()
  }
}
