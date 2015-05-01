package com.eharmony.matching.featureSpecExtractor.json

import scala.collection.{immutable => sci}

//case class JsonSpec(
//        imports: Seq[String],
//        features: sci.IndexedSeq[Spec],
//        namespaces: Option[Seq[Namespace]] = None,
//        label: Option[String] = None,
//        importance: Option[String] = None,
//        cbAction: Option[String] = None,
//        cbCost: Option[String] = None,
//        cbProbability: Option[String] = None,
//        normalizeFeatures: Option[Boolean] = Some(false),
//        specType: Option[String],
//        numBits: Option[Int]) {
//
//
//}
//
//object JsonSpec extends AlohaReadable[JsonSpec] with ReadableByString[JsonSpec] with DefaultJsonProtocol {
//
//    private[this] implicit val namespaceFormat = jsonFormat2(Namespace)
//    private[this] implicit val specFormat = jsonFormat3(Spec)
//    private[this] implicit val jsonSpecFormat = jsonFormat11(JsonSpec.apply)
//
//    /** Read from a String.
//      * @param s a String to read.
//      * @return the result
//      */
//    override def fromString(s: String) = s.parseJson.convertTo[JsonSpec]
//}
