package com.eharmony.matching.aloha.io.sources

import java.io.{Reader, InputStream, File}
import java.net.URL

import org.apache.commons.{vfs2, vfs}

/** Provides a bunch of explicit and implicit [[com.eharmony.matching.aloha.io.sources.ReadableSourceConverter]]s.
  *
  * All of the ''implicit'' converters in [[com.eharmony.matching.aloha.io.sources.ReadableSourceConverters.Implicits]]
  * can be imported without worry.
  *
  * All converters in [[com.eharmony.matching.aloha.io.sources.ReadableSourceConverters.StringImplicits]] take the same
  * input type (''String'') so only one should be imported into the implicit scope at once.
  *
  * Explicit versions of the converters are available for use and delegate to the appropriate implicit converter.
  */
object ReadableSourceConverters {

    def fileReadableConverter: ReadableSourceConverter[File] = Implicits.fileReadableConverter
    def urlReadableConverter: ReadableSourceConverter[URL] = Implicits.urlReadableConverter
    def vfs1ReadableConverter: ReadableSourceConverter[vfs.FileObject] = Implicits.vfs1ReadableConverter
    def vfs2ReadableConverter: ReadableSourceConverter[vfs2.FileObject] = Implicits.vfs2ReadableConverter
    def inputStreamReadableConverter: ReadableSourceConverter[InputStream] = Implicits.inputStreamReadableConverter
    def readerReadableConverter: ReadableSourceConverter[Reader] = Implicits.readerReadableConverter

    def stringToClasspathResourceReadableConverter: ReadableSourceConverter[String] = StringImplicits.stringToClasspathResourceReadableConverter
    def stringToResourceReadableConverter: ReadableSourceConverter[String] = StringImplicits.stringToResourceReadableConverter
    def stringToStringReadableConverter: ReadableSourceConverter[String] = StringImplicits.stringToStringReadableConverter

    /** Can do a blanket import of values in this object:
      * {{{
      * import com.eharmony.matching.aloha.io.sources.ReadableSourceConverters.Implicits._
      * }}}
      */
    object Implicits {
        implicit val fileReadableConverter: ReadableSourceConverter[File] = (f: File) => FileReadableSource(f)
        implicit val urlReadableConverter: ReadableSourceConverter[URL] = (u: URL) => UrlReadableSource(u)
        implicit val vfs1ReadableConverter: ReadableSourceConverter[vfs.FileObject] = (fo: vfs.FileObject) => Vfs1ReadableSource(fo)
        implicit val vfs2ReadableConverter: ReadableSourceConverter[vfs2.FileObject] = (fo: vfs2.FileObject) => Vfs2ReadableSource(fo)
        implicit val inputStreamReadableConverter: ReadableSourceConverter[InputStream] = (is: InputStream) => InputStreamReadableSource(is)
        implicit val readerReadableConverter: ReadableSourceConverter[Reader] = (r: Reader) => ReaderReadableSource(r)
    }

    /** '''SHOULD NOT DO''' do a blanket import of values in this object!  Instead, do something like:
      * {{{
      * import com.eharmony.matching.aloha.io.sources.ReadableSourceConverters.StringImplicits.stringToStringReadableConverter
      * }}}
      */
    object StringImplicits {
        implicit val stringToClasspathResourceReadableConverter: ReadableSourceConverter[String] = (s: String) => ClasspathResourceReadableSource(s)
        implicit val stringToResourceReadableConverter: ReadableSourceConverter[String] = (s: String) => ResourceReadableSource(s)
        implicit val stringToStringReadableConverter: ReadableSourceConverter[String] = (s: String) => StringReadableSource(s)
    }
}
