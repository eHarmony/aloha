package com.eharmony.aloha.models

import com.eharmony.aloha.reflect._
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenFunc0

import scala.util.Try

/**
  * Created by jmorra on 2/29/16.
  */
object AnySemanticsWithoutFunctionCreation extends Semantics[Any] {
  def refInfoA: RefInfo[Any] = RefInfo[Any]
  def accessorFunctionNames: Seq[Nothing] = Nil
  def close(): Unit = {}
  def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenFunc0[Any, B]] = {
    val right = Try {
      val long = codeSpec.toLong
      Right(GenFunc0(codeSpec, (a: Any) => long.asInstanceOf[B]))
    }
    right.getOrElse(Left(Seq("createFunction not supported.")))
  }
}
