package com.eharmony.matching.aloha.io

import java.io.{InputStreamReader, ByteArrayOutputStream, Reader, InputStream}
import org.apache.commons.io.IOUtils

/** Support for various ways of reading data by converting the input type to a String and calling fromString.
  *
  * @tparam A the result type produced by reading from one of the readable formats.
  */
trait ReadableByString[A] extends ReadableCommon[A] {

    /** Read from an InputStream.
      *
      * @param is an InputStream to read.  The InputStream is automatically closed.
      * @return the result
      */
    def fromInputStream(is: InputStream): A = {
        try {
            fromReader(new InputStreamReader(is))
        }
        finally {
            IOUtils.closeQuietly(is)
        }
    }

    /** Read from a Reader.  Whoever extends this trait is responsible for closing the Reader.
      *
      * @param r a Reader containing data to be read.
      * @return the result
      */
    def fromReader(r: Reader): A = {
        try {
            val baos = new ByteArrayOutputStream  // Don't need to close.
            IOUtils.copy(r, baos, inputCharset)
            fromString(new String(baos.toByteArray))
        }
        finally {
            IOUtils.closeQuietly(r)
        }
    }
}
