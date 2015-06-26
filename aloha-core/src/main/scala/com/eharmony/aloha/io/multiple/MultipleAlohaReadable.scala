package com.eharmony.aloha.io.multiple

import com.eharmony.aloha.io.AlohaReadable
import com.eharmony.aloha.io.sources._

/** Allows us to read in bulk from file-oriented and non-file-oriented resources by delegating to
  * [[com.eharmony.aloha.io.AlohaReadable]].
  *
  * @tparam A the readable sources.  This allows objects that mix in only a subset of Readable interfaces to have type
  *           safety when trying to parse multiple sources.  This allows the programmer to use the type system instead
  *           of needing to deal with cases that shouldn't exists.  For instance, imagine implementing just
  *           NonFileReadable.  Then, we shouldn't have to test for cases where the readable source refers to a File.
  *           The type hierarchy of ReadableSource allows this.
  */
trait MultipleAlohaReadable[A] extends MultipleReadable[ReadableSource, A]{ self: AlohaReadable[A] =>

    val mapper = (_: ReadableSource) match {
        case FileReadableSource(file) => fromFile(file)
        case UrlReadableSource(url) => fromUrl(url)
        case Vfs1ReadableSource(vfs1) => fromVfs1(vfs1)
        case Vfs2ReadableSource(vfs2) => fromVfs2(vfs2)
        case ResourceReadableSource(resource) => fromResource(resource)
        case ClasspathResourceReadableSource(resource) => fromClasspathResource(resource)

        case StringReadableSource(s) => fromString(s)
        case InputStreamReadableSource(is) => fromInputStream(is)
        case ReaderReadableSource(r) => fromReader(r)
    }
}
