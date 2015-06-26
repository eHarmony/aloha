package com.eharmony.aloha.io

import java.io.{InputStreamReader, InputStream, StringReader}
import org.apache.commons.io.IOUtils

trait ReadableByReader[A] extends AlohaReadable[A] with FileReadableByInputStream[A] {

    /** Read from an InputStream.
      * @param is an InputStream to read.  The InputStream is automatically closed.
      * @return the result
      */
    final def fromInputStream(is: InputStream): A = {
        try {
            fromReader(new InputStreamReader(is))
        }
        finally {
            IOUtils.closeQuietly(is)
        }
    }

    /** Read from a String.  Whoever extends this trait is responsible for closing the Reader.
      * @param s a String containing data to be read.
      * @return the result
      */
    final def fromString(s: String): A = fromReader(new StringReader(s))
}
