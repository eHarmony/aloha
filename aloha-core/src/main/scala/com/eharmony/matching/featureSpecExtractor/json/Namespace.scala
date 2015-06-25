package com.eharmony.matching.featureSpecExtractor.json

import spray.json.pimpAny
import spray.json.{RootJsonWriter, DefaultJsonProtocol}

case class Namespace(name: String, features: Seq[String])

object Namespace extends DefaultJsonProtocol {
    implicit val namespaceFormat = jsonFormat2(Namespace.apply)
    val modelNamespaceWriter = new RootJsonWriter[Namespace] {
        override def write(ns: Namespace) = ns.features.toJson
    }
}
