package com.eharmony.aloha.reflect

import java.{lang => jl}

import deaktator.reflect.runtime.manifest.ManifestParser

object RefInfo {
  val Any:           RefInfo[Any]          = RefInfoOps.refInfo[Any]
  val AnyRef:        RefInfo[AnyRef]       = RefInfoOps.refInfo[AnyRef]
  val AnyVal:        RefInfo[AnyVal]       = RefInfoOps.refInfo[AnyVal]
  val Boolean:       RefInfo[Boolean]      = RefInfoOps.refInfo[Boolean]
  val Byte:          RefInfo[Byte]         = RefInfoOps.refInfo[Byte]
  val Char:          RefInfo[Char]         = RefInfoOps.refInfo[Char]
  val Double:        RefInfo[Double]       = RefInfoOps.refInfo[Double]
  val Float:         RefInfo[Float]        = RefInfoOps.refInfo[Float]
  val Int:           RefInfo[Int]          = RefInfoOps.refInfo[Int]
  val JavaBoolean:   RefInfo[jl.Boolean]   = RefInfoOps.refInfo[jl.Boolean]
  val JavaByte:      RefInfo[jl.Byte]      = RefInfoOps.refInfo[jl.Byte]
  val JavaCharacter: RefInfo[jl.Character] = RefInfoOps.refInfo[jl.Character]
  val JavaDouble:    RefInfo[jl.Double]    = RefInfoOps.refInfo[jl.Double]
  val JavaFloat:     RefInfo[jl.Float]     = RefInfoOps.refInfo[jl.Float]
  val JavaInteger:   RefInfo[jl.Integer]   = RefInfoOps.refInfo[jl.Integer]
  val JavaLong:      RefInfo[jl.Long]      = RefInfoOps.refInfo[jl.Long]
  val JavaShort:     RefInfo[jl.Short]     = RefInfoOps.refInfo[jl.Short]
  val Long:          RefInfo[Long]         = RefInfoOps.refInfo[Long]
  val Nothing:       RefInfo[Nothing]      = RefInfoOps.refInfo[Nothing]
  val Null:          RefInfo[Null]         = RefInfoOps.refInfo[Null]
  val Object:        RefInfo[AnyRef]       = RefInfoOps.refInfo[AnyRef]
  val Short:         RefInfo[Short]        = RefInfoOps.refInfo[Short]
  val Unit:          RefInfo[Unit]         = RefInfoOps.refInfo[Unit]
  val String:        RefInfo[String]       = RefInfoOps.refInfo[String]

  def apply[A: RefInfo]: RefInfo[A] = RefInfoOps.refInfo[A]

  /**
    * Create an untyped RefInfo from `strRep`.
    * @param strRep a string representation of the Manifest.
    * @return Either an error message on the Left in the event of a failure or a `RefInfo` on
    *         the Right for a success.
    */
  def fromString(strRep: String): Either[String, Manifest[_]] = {
    // TODO: Fix this in ManifestParser.  This is a stopgap solution.
    if ("String" == strRep.trim)
      Right(this.String)
    else ManifestParser.parse(strRep)
  }
}
