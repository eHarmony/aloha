package com.eharmony.aloha.io.multiple

import com.eharmony.aloha.io.FileReadable
import com.eharmony.aloha.io.sources._


/** Allows us to read in bulk from file-oriented resources by delegating to
  * [[com.eharmony.aloha.io.FileReadable]].
  *
  * @tparam A the readable sources.  This allows objects that mix in only a subset of Readable interfaces to have type
  *           safety when trying to parse multiple sources.  This allows the programmer to use the type system instead
  *           of needing to deal with cases that shouldn't exists.  For instance, imagine implementing just
  *           NonFileReadable.  Then, we shouldn't have to test for cases where the readable source refers to a File.
  *           The type hierarchy of ReadableSource allows this.
  */
trait MultipleFileReadable[A] extends MultipleReadable[FileReadableLikeSource, A] { self: FileReadable[A] =>
    val mapper = (_: FileReadableLikeSource) match {
        case FileReadableSource(file) => fromFile(file)
        case UrlReadableSource(url) => fromUrl(url)
        case Vfs1ReadableSource(vfs1) => fromVfs1(vfs1)
        case Vfs2ReadableSource(vfs2) => fromVfs2(vfs2)
        case ResourceReadableSource(resource) => fromResource(resource)
        case ClasspathResourceReadableSource(resource) => fromClasspathResource(resource)
    }
}
