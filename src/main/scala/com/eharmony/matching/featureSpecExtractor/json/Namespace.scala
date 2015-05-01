package com.eharmony.matching.featureSpecExtractor.json

import spray.json.DefaultJsonProtocol

case class Namespace(name: String, features: Seq[String])

object Namespace extends DefaultJsonProtocol {
    implicit val namespaceFormat = jsonFormat2(Namespace.apply)
}
