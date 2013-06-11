package com.eharmony.matching.aloha.io

object StringReadable extends ReadableByString[String] {
    def fromString(s: String) = s
}
