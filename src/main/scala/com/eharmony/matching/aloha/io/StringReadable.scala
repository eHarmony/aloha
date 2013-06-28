package com.eharmony.matching.aloha.io

/** Provides a very easy way to read data input a string.
  */
object StringReadable extends ReadableByString[String] with GZippedReadable[String] {
    def fromString(s: String) = s
}
