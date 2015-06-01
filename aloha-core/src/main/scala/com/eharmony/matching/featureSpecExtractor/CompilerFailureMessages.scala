package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.AlohaException

trait CompilerFailureMessages {
    protected[this] final def failure(featureName: String, msgs: Seq[String]) = {
        val firstWhitespace = if (msgs.nonEmpty) "\n\t" else ""
        new AlohaException(s"Couldn't compile function $featureName: $firstWhitespace ${msgs.mkString("\n\t")}")
    }
}
