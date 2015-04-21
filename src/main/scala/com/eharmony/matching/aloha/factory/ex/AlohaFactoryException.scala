package com.eharmony.matching.aloha.factory.ex

import com.eharmony.matching.aloha.AlohaException

class AlohaFactoryException(message: String, cause: Throwable) extends AlohaException(message, cause) {
    def this() = this(null, null)
    def this(message: String) = this(message, null)
    def this(cause: Throwable) = this(null, cause)
}
