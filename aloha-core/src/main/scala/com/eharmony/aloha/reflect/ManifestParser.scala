package com.eharmony.aloha.reflect

import scala.reflect.ManifestFactory
import scala.util.parsing.combinator.RegexParsers


/**
  * Parser that takes string input and constructs `scala.reflect.Manifest` instances.
  * {{{
  * // Example usages:
  *
  * // Either[String,scala.reflect.Manifest[_]]
  * val em = ManifestParser.parse("scala.collection.immutable.List[scala.Double]")
  * assert("scala.collection.immutable.List[Double]" == em.right.get.toString)
  *
  * val em2 = ManifestParser.parse("scala.collection.immutable.Map[Int, scala.Float]")
  * assert("scala.collection.immutable.Map[Int, Float]" == em2.right.get.toString)
  * }}}
  *
  * @author deaktator
  */
object ManifestParser {
  import Grammar.{Success, Error, Failure}

  /**
    * Parse a string representation of a typed `Manifest`.
    * Example":
    * {{{
    * ManifestParser.parse("scala.collection.immutable.Map[Int, scala.Float]").right.get
    * }}}
    * @param strRep a string representation of the `Manifest`.
    * @return either an error on the left or a `Manifest` (missing it's type parameter)
    *         on the right
    */
  def parse(strRep: CharSequence): Either[String, Manifest[_]] = {
    try {
      Grammar.parseAll(Grammar.manifests, strRep) match {
        case Success(man, _)    => Right(man)
        case Error(err, next)   => Left(errorMsg(strRep, err, next))
        case Failure(err, next) => Left(errorMsg(strRep, err, next))
      }
    }
    catch {
      case ex: ClassNotFoundException =>
        Left(s"For $strRep, class not found: ${ex.getMessage}")
    }
  }

  /**
    * Provides an error reporting message.
    * @param strRep string representation of the `Manifest` that should have been created.
    * @param err the error message returned by the parser.
    * @param next the rest of the input remaining at the location of the error.
    * @return an error message.
    */
  private[this] def errorMsg(strRep: CharSequence, err: String, next: Grammar.Input): String = {
    val whiteSpace = Seq.fill(next.offset + 1)(" ").mkString("")
    s"Problem at character ${next.offset}:\n'$strRep\n$whiteSpace^\n$err"
  }

  /**
    * A grammar for matching scala Manifests.
    * This is separated from the rest of the module code for readability.
    *
    * @author deaktator
    */
  private[this] object Grammar extends RegexParsers {

    /**
      * The root production rule.
      * This is typed for two reasons:
     - ''it is the root production''
     - `withArg` ''and'' `array` ''rules refer to this rule, making it recursively thereby necessitating the type.''
      */
    lazy val manifests: Parser[Manifest[_]] = array | withArg | noArg

    /**
      * Production rule for manifests with (possibly multiple) type parameters.
      */
    lazy val withArg = path ~ (oBracket ~> rep1sep(manifests, ",") <~ cBracket) ^^ {
      case p ~ params => ManifestParser.parameterizedManifest(p, params)
    }

    /**
      * Production rule for Array types.
      * Arrays can only have one type parameter.
      */
    lazy val array = arrayToken ~> oBracket ~> manifests <~ cBracket ^^ {
      ManifestParser.arrayManifest
    }

    lazy val noArg = special | classManifest

    lazy val classManifest = path ^^ { ManifestParser.classManifest }

    lazy val special = opt(scalaPkg) ~> (obj | anyRef | anyVal | any | nothing | anyVals)

    lazy val obj     = "Object"  ^^^ ManifestFactory.Object
    lazy val any     = "Any"     ^^^ ManifestFactory.Any
    lazy val anyVal  = "AnyVal"  ^^^ ManifestFactory.AnyVal
    lazy val anyRef  = "AnyRef"  ^^^ ManifestFactory.AnyRef
    lazy val nothing = "Nothing" ^^^ ManifestFactory.Nothing

    lazy val anyVals = boolean | byte | char | double | float | int | long | short | unit

    lazy val boolean = "Boolean" ^^^ ManifestFactory.Boolean
    lazy val byte    = "Byte"    ^^^ ManifestFactory.Byte
    lazy val char    = "Char"    ^^^ ManifestFactory.Char
    lazy val double  = "Double"  ^^^ ManifestFactory.Double
    lazy val float   = "Float"   ^^^ ManifestFactory.Float
    lazy val int     = "Int"     ^^^ ManifestFactory.Int
    lazy val long    = "Long"    ^^^ ManifestFactory.Long
    lazy val short   = "Short"   ^^^ ManifestFactory.Short
    lazy val unit    = "Unit"    ^^^ ManifestFactory.Unit

    lazy val path = """[a-zA-Z_][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)*""".r
    lazy val oBracket = "["
    lazy val cBracket = "]"
    lazy val scalaPkg = """(scala\.)?""".r

    /**
      * An array token should only match the remaining input starting from its beginning.
      */
    // TODO: Determine if the '^' prefix is necessary.
    lazy val arrayToken = """^(scala\.)?Array""".r
  }

  // vvvvvvvvvv   Functions for transforming input to Manifest instances.   vvvvvvvvvv
  // TODO: These should be package private for testing.

  private[reflect] def classManifest(tpe: String): Manifest[_] =
    ManifestFactory.classType(Class.forName(tpe))

  private[reflect] def arrayManifest(tpe: Manifest[_]) = ManifestFactory.arrayType(tpe)

  private[reflect] def parameterizedManifest(tpe: String, typeParams: Seq[Manifest[_]]): Manifest[_] = {
    val clas = Class.forName(tpe)
    val (first, rest) = typeParams.splitAt(1)
    ManifestFactory.classType(clas, first.head, rest:_*)
  }
}
