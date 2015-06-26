package com.eharmony.aloha.io

import com.eharmony.aloha.util.Logging

/**
 * Provides a very easy way to read data input a string.
 */
object StringReadable extends ReadableByString[String] with GZippedReadable[String] with LocationLoggingReadable[String] with Logging {
  def fromString(s: String) = s
}
