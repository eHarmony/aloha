package com.eharmony.matching.aloha.io

import java.io.{Reader, InputStream}

trait NonFileReadable[A] {

    /** Read from a String.
      * @param s a String to read.
      * @return the result
      */
    def fromString(s: String): A

    /** Read from an InputStream.
      * @param is an InputStream to read.  The InputStream is automatically closed.
      * @return the result
      */
    def fromInputStream(is: InputStream): A

    /** Read from an Reader.
      * @param r a Reader from which to read.  The Reader is automatically closed.
      * @return the result
      */
    def fromReader(r: Reader): A
}
