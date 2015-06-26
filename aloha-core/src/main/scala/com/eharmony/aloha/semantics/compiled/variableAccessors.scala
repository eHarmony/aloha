package com.eharmony.aloha.semantics.compiled

sealed trait VariableAccessorCode {
    val body: Seq[String]
    val isOptional: Boolean
    def compressed = body.map(_.trim).mkString(" ")
    def pretty = body.mkString(System.getProperty("line.separator", "\n"))
}

case class RequiredAccessorCode(body: Seq[String]) extends VariableAccessorCode {
    val isOptional = false
}

case class OptionalAccessorCode(body: Seq[String]) extends VariableAccessorCode {
    val isOptional = true
}
