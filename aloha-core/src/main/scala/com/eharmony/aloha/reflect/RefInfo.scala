package com.eharmony.aloha.reflect

import java.{lang => jl}

import deaktator.reflect.runtime.manifest.ManifestParser

object RefInfo {
    val Any = RefInfoOps.refInfo[Any]
    val AnyRef = RefInfoOps.refInfo[AnyRef]
    val AnyVal = RefInfoOps.refInfo[AnyVal]
    val Boolean = RefInfoOps.refInfo[Boolean]
    val Byte = RefInfoOps.refInfo[Byte]
    val Char = RefInfoOps.refInfo[Char]
    val Double = RefInfoOps.refInfo[Double]
    val Float = RefInfoOps.refInfo[Float]
    val Int = RefInfoOps.refInfo[Int]
    val JavaBoolean = RefInfoOps.refInfo[jl.Boolean]
    val JavaByte = RefInfoOps.refInfo[jl.Byte]
    val JavaCharacter = RefInfoOps.refInfo[jl.Character]
    val JavaDouble = RefInfoOps.refInfo[jl.Double]
    val JavaFloat = RefInfoOps.refInfo[jl.Float]
    val JavaInteger = RefInfoOps.refInfo[jl.Integer]
    val JavaLong = RefInfoOps.refInfo[jl.Long]
    val JavaShort = RefInfoOps.refInfo[jl.Short]
    val Long = RefInfoOps.refInfo[Long]
    val Nothing = RefInfoOps.refInfo[Nothing]
    val Null = RefInfoOps.refInfo[Null]
    val Object = RefInfoOps.refInfo[AnyRef]
    val Short = RefInfoOps.refInfo[Short]
    val Unit = RefInfoOps.refInfo[Unit]
    val String = RefInfoOps.refInfo[String]
    def apply[A: RefInfo] = RefInfoOps.refInfo[A]

  /**
    * Create an untyped RefInfo from `strRep`.
    * @param strRep a string representation of the Manifest.
    * @return Either an error message on the Left in the event of a failure or a `RefInfo` on
    *         the Right for a success.
    */
    def fromString(strRep: String): Either[String, RefInfo[_]] =
      ManifestParser.parse(strRep)
}
