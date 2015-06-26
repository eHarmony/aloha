package com.eharmony.aloha.io.multiple

import scala.language.higherKinds

import scalaz.{Traverse, Applicative}
import com.eharmony.aloha.io.sources.ReadableSource

/** This mixin provides a way to sequence the multiple readables whose return value is a container of some sort.  For
  * more info, see
  * A [[http://www.haskell.org/haskellwiki/Foldable_and_Traversable#Traversable Traversable from Haskell wiki]]
  * or look up ''sequence'', etc.  For now, let's provide an example.  Assuming we have an object StrToIntList that
  * takes a string and produces a try of a list of integers by splitting and then parsing the ints.  The result is
  * either a success or failure but either way, the result is wrapped in a try container.  Notice, that we included in
  * the definition, '''with SequenceMultipleReadable[ReadableSource, Try, List[Int]]'''.  This is all that is
  * required at definition time to get the sequencing that we want.
  *
  * The calling code (once the proper type class instances are in imported into the implicit scope) simply passes the
  * container of objects to ''fromSequencedMultipleSources'' and the result is a '''Try[Stream[List[Int]]]'''.
  *
  * {{{
  * object StrToIntList
  *   extends ReadableByString[Try[List[Int]]]
  *   with MultipleAlohaReadable[Try[List[Int]]]
  *   with SequenceMultipleReadable[ReadableSource, Try, List[Int]] {
  *
  *   def fromString(s: String): Try[List[Int]] = Try {
  *     if (s.trim == "") List.empty
  *     else s.trim.split("""\s*,\s*""", -1).map(_.toInt).toList
  *   }
  * }
  *
  * // ======   Calling code   ==============================================================================
  *
  * import scalaz.std.stream.streamInstance                  // Provides (non-strict) Traverse type class for Stream
  * import com.eharmony.aloha.util.TryScalazMonad   // Provides Applicative type class for Try
  *
  * val in = Stream("1, 11, 111",
  *                 "2, BAD",
  *                 "3, 33, 333")
  *
  * // This is the thing to notice:  we get a Try[Stream[List[Int]]] and not a Stream[Try[List[Int]]].  This is
  * // why this function is here.  It commutes the container types.  And because the Stream is non-strict, there is no
  * // attempt to parse the 3rd string "3, 33, 333" since an error occurred before getting to the third element.
  * // This behaviour can change for strict Traverse object (like the Traverse for List).
  * //
  * val out: Try[Stream[List[Int]]] = StrToIntList.fromSequencedMultipleSources(in)
  *
  * // Test the error occurred.
  * val exceptionMsg = """For input string: "BAD""""
  * require(out.isFailure && exceptionMsg == out.failed.get.getMessage, "Should have failed.")
  * }}}
  *
  * @tparam APP A applicative functor container type (see [[http://thedet.wordpress.com/2012/04/28/functors-monads-applicatives-can-be-so-simple/ Functors, Monads, Applicatives – can be so simple]]).
  * @tparam B The container type in the output
  */
trait SequenceMultipleReadable[A <: ReadableSource, APP[_], B] { self: MultipleReadable[A, APP[B]] =>

    /** Produce multiple outputs from multiple inputs and sequence the data so that we commute that container types.
      * For an example, see that trait documentation.
      * @param readableTypes A container of readable sources to convert to outputs.
      * @param t a Traverse type class instance
      * @param ap an applicative function type class instance
      * @tparam T the Traverse container type
      * @return
      */
    def fromSequencedMultipleSources[T[_]](readableTypes: T[A])(implicit t: Traverse[T], ap: Applicative[APP]): APP[T[B]] =
        t.traverse(readableTypes)(mapper)(ap)
}
