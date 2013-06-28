package com.eharmony.matching.aloha.io

import java.io.InputStream
import java.util.zip.GZIPInputStream

/** When mixed in with a [[com.eharmony.matching.aloha.io.NonFileReadable]][A], this will provide a method ''gz'' that
  * will read [[http://www.gzip.org/ gzipped]] versions of files.  This works by creating a private object that
  * implements [[com.eharmony.matching.aloha.io.FileReadableByInputStream]] with a ''fromInputStream'' method that
  * wraps the incoming InputStream in a
  * [[http://docs.oracle.com/javase/6/docs/api/java/util/zip/GZIPInputStream.html java.util.zip.GZIPInputStream]] and
  * then forwards the wrapped InputStream to the ''fromInputStream'' of the object into which GZippedReadable was mixed.
  *
  * This gives all the different ways to unpack gzipped data from a File, URL, FileObject, etc.  For instance:
  *
  * {{{
  * class X[Seq[String]] extends ReadableByString[Seq[String]] with GZippedReadable[Seq[String]] {
  *   def fromString(s: String) = s.split("\t", -1)
  *   def doSomething() {
  *     val zippedResourcePath = "..."
  *
  *     // Get the Seq[String] from resource and do stuff to it...
  *     gz.fromResource(zippedResourcePath).filter(_.size > 1).size
  *   }
  * }
  * }}}
  *
  * @tparam A
  */
trait GZippedReadable[A] { self: NonFileReadable[A] =>
    private[this] object gzip extends FileReadableByInputStream[A] {
        def fromInputStream(is: InputStream) = self.fromInputStream(new GZIPInputStream(is))
    }
    def gz(): FileReadable[A] = gzip
}
