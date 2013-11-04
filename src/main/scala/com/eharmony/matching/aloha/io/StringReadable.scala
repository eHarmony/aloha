package com.eharmony.matching.aloha.io

import grizzled.slf4j.Logging

/** Provides a very easy way to read data input a string.
  */
object StringReadable extends ReadableByString[String] with GZippedReadable[String] with LocationLoggingReadable[String] with Logging {
    def fromString(s: String) = s
}
