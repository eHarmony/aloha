package com.eharmony.aloha.semantics

import com.eharmony.aloha.reflect.RefInfo

case class NoSemantics[A: RefInfo]() extends Semantics[A] {
  override def refInfoA: RefInfo[A] = RefInfo[A]
  override def accessorFunctionNames: Seq[String] = Nil
  override def createFunction[B: RefInfo](codeSpec: String, default: Option[B]) = Left(Nil)
  override def close(): Unit = {}
}
