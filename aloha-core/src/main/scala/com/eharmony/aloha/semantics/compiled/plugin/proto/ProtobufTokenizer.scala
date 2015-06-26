package com.eharmony.aloha.semantics.compiled.plugin.proto

import scala.util.parsing.combinator.JavaTokenParsers

import scalaz.ValidationNel
import scalaz.syntax.validation.ToValidationV // scalaz.syntax.validation.ToValidationOps for latest scalaz

/**
 * This tokenizer extracts sequences of Field and Index tokens which are then used in conjunction with the reflection
 * APIs in the Protocol Buffers library to synthesize code for high-performance, type-safe accessor functions.
 */
private[this] object ProtobufTokenizer {
  private val tokenizer = Tokenizer("[", "]")

  val leftBracket = tokenizer.leftBracket
  val rightBracket = tokenizer.leftBracket

  /**
   * Get a list of tokens on success or an error message on failure.  Note that the grammar in tokenizer
   * requires that sequence of tokens produced must be like: ''(Field Index?)+''. This implies the sequence:
   *
   * 1.  must contain a Field token at the start
   * 1.  must contain at least as many Field tokens as Index tokens
   * 1.  must NOT contain two adjacent Index tokens
   *
   * @param cs A string to parse
   * @return either a list of tokens on success, or an error message on failure.
   */
  def getTokens(cs: CharSequence): ValidationNel[String, List[Token]] = tokenizer.parseAll(tokenizer.spec, cs) match {
    case s if s.successful => s.get.success
    case f => f.toString.failNel
  }

  /**
   * A parser combinator-based tokenizer for use in conjunction with the Protocol Buffer reflection APIs.  This
   * extracts the variable names that will checked against a concrete implementation of a GeneratedMessage to see
   * if the desired field can be extracted.  If it can, then an accessor function will be synthesized.
   *
   * @param leftBracket
   * @param rightBracket
   */
  private[proto] case class Tokenizer(leftBracket: String, rightBracket: String) extends JavaTokenParsers {

    /**
     * Create a parser that consumes an entire string and returns a list of Token instances that contains a
     * specification for an accessor function.  Note, this accessor may not exist in a given Protocol Buffer
     * implementation but it is structurally correct.
     */
    lazy val spec = phrase(rep1sep(oneLevel, ".")) ^^ { _.flatten }

    /**
     * Parser for one level in the object tree.  This may or may not include the dereferencing of a list.  This
     * parser returns a list of 1 or 2 tokens.
     */
    lazy val oneLevel = ident ~ opt(index) ^^ { case x ~ i => Field(x) :: (for { n <- i } yield List(Index(n))).getOrElse(Nil) }

    /**
     * A parser that consumes a left bracket token, an integer token and a right bracket token and returns the
     * integer version of the integer string contained between the brackets.
     */
    lazy val index = leftBracket ~> nonNegInt <~ rightBracket ^^ { _.toInt }

    /**
     * Non-negative integer parser used for extracting offsets in list structures.
     */
    val nonNegInt = """\d+""".r
  }
}
