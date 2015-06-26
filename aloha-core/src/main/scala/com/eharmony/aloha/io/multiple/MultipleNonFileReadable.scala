package com.eharmony.aloha.io.multiple

import com.eharmony.aloha.io.NonFileReadable
import com.eharmony.aloha.io.sources.{ReaderReadableSource, InputStreamReadableSource, StringReadableSource, NonFileReadableLikeSource}

/** Allows us to read in bulk from non-file-oriented resources by delegating to
  * [[com.eharmony.aloha.io.NonFileReadable]].
  *
  * @tparam A the readable sources.  This allows objects that mix in only a subset of Readable interfaces to have type
  *           safety when trying to parse multiple sources.  This allows the programmer to use the type system instead
  *           of needing to deal with cases that shouldn't exists.  For instance, imagine implementing just
  *           NonFileReadable.  Then, we shouldn't have to test for cases where the readable source refers to a File.
  *           The type hierarchy of ReadableSource allows this.
  */
trait MultipleNonFileReadable[A] extends MultipleReadable[NonFileReadableLikeSource, A] { self: NonFileReadable[A] =>
    val mapper = (_: NonFileReadableLikeSource) match {
        case StringReadableSource(s) => fromString(s)
        case InputStreamReadableSource(is) => fromInputStream(is)
        case ReaderReadableSource(r) => fromReader(r)
    }
}
