package com.eharmony.aloha.io.sources

import java.io.{Reader, InputStream, File}
import java.net.URL
import org.apache.commons.{vfs, vfs2}

/** An object from which we can read in bulk.
  */
sealed trait ReadableSource

/** An object from which we can read in bulk using a [[com.eharmony.aloha.io.FileReadable]].
  */
sealed trait FileReadableLikeSource extends ReadableSource

/** An object from which we can read in bulk using a [[com.eharmony.aloha.io.NonFileReadable]].
  */
sealed trait NonFileReadableLikeSource extends ReadableSource

case class FileReadableSource(file: File) extends FileReadableLikeSource
case class UrlReadableSource(url: URL) extends FileReadableLikeSource
case class Vfs1ReadableSource(vfs1FileObject: vfs.FileObject) extends FileReadableLikeSource
case class Vfs2ReadableSource(vfs2FileObject: vfs2.FileObject) extends FileReadableLikeSource
case class ResourceReadableSource(resource: String) extends FileReadableLikeSource
case class ClasspathResourceReadableSource(resource: String) extends FileReadableLikeSource

case class StringReadableSource(string: String) extends NonFileReadableLikeSource
case class InputStreamReadableSource(inputStream: InputStream) extends NonFileReadableLikeSource
case class ReaderReadableSource(reader: Reader) extends NonFileReadableLikeSource
