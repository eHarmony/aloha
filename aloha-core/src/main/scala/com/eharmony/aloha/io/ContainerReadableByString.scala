package com.eharmony.aloha.io

import scala.language.higherKinds
import org.apache.commons.io.IOUtils
import java.lang.String
import java.io.{ByteArrayOutputStream, Reader, InputStreamReader, InputStream}

/** Support for various ways of reading data by converting the input type to a String and calling fromString.
  */
trait ContainerReadableByString[C[_]] extends ContainerReadableCommon[C] {

    /** Read from an InputStream.
      *
      * @param is an InputStream to read.  The InputStream is automatically closed.
      * @return the result
      */
    def fromInputStream[A](is: InputStream): C[A] = {
        try {
            fromReader[A](new InputStreamReader(is))
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
    def fromReader[A](r: Reader): C[A] = {
        try {
            val baos = new ByteArrayOutputStream  // Don't need to close.
            IOUtils.copy(r, baos, inputCharset)
            fromString[A](new String(baos.toByteArray))
        }
        finally {
            IOUtils.closeQuietly(r)
        }
    }
}
