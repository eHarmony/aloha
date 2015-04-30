package com.eharmony.matching.featureSpecExtractor.json

import com.eharmony.matching.aloha.io.{ReadableByString, AlohaReadable}
import spray.json._

import scala.collection.{SeqView, immutable => sci}

case class JsonSpec(
        imports: Seq[String],
        features: sci.IndexedSeq[Spec],
        namespaces: Option[Seq[Namespace]] = None,
        label: Option[String] = None,
        importance: Option[String] = None,
        cbAction: Option[String] = None,
        cbCost: Option[String] = None,
        cbProbability: Option[String] = None,
        normalizeFeatures: Option[Boolean] = Some(false),
        specType: Option[String],
        numBits: Option[Int]) {

    final def validate(): Option[String] = {
        lazy val dupFeatNames = findDupicates(features.view)(_.name)
        lazy val ns = namespaces.getOrElse(Seq.empty)
        lazy val dupNsNames = findDupicates(ns.view)(_.name)
        lazy val dupNsFeats = ns.flatMap{ n =>
            val x = findDupicates(n.features.view)(identity)
            if (x.nonEmpty) Seq((n.name, x)) else Seq.empty
        }

        if (dupFeatNames.nonEmpty) Option(s"duplicate feature names detected: $dupFeatNames.")
        else if (dupNsNames.nonEmpty) Option(s"duplicate namespace names detected: $dupNsNames.")
        else if (dupNsFeats.nonEmpty) Option(s"duplicate namespace features detected: $dupNsFeats")
        else None
    }

    /**
     * Find duplicates in xs based on some criteria.
     * @param xs where to look for duplicates
     * @param criteria the criteria used to determine duplicates.
     * @tparam A type of
     * @tparam K
     * @return
     */
    private[this] final def findDupicates[A, K](xs: SeqView[A, Seq[A]])(criteria: A => K) =
        xs.groupBy(criteria).collect{case (k, v) if v.size > 1 => k }(collection.breakOut)

    /**
     * Get the default namespace index mapping and the mapping from each namespace name to the feature index.
     * @return
     */
    final def namespaceIndices(): (sci.IndexedSeq[Int], sci.IndexedSeq[(String, sci.IndexedSeq[Int])]) = {
        // Mapping from feature name to feature index.
        val fMap = features.view.zipWithIndex.map{case(k, v) => (k.name, v)}.toMap

        // Mapping from namespace name to sequence of feature indices.
        val nss = namespaces.getOrElse(Seq.empty).map(ns => (ns.name, ns.features.flatMap(fMap.get).toIndexedSeq)).toIndexedSeq

        // default (unnamed) namespace mapping.  These are the indices not in any namespace.  Sorted.
        val default = nss.foldLeft(features.indices.toSet)((ind, ns) => ind -- ns._2).toIndexedSeq.sorted
        (default, nss)
    }

    def shouldNormalizeFeatures: Boolean = normalizeFeatures.getOrElse(false)
}

object JsonSpec extends AlohaReadable[JsonSpec] with ReadableByString[JsonSpec] with DefaultJsonProtocol {

    private[this] implicit val namespaceFormat = jsonFormat2(Namespace)
    private[this] implicit val specFormat = jsonFormat3(Spec)
    private[this] implicit val jsonSpecFormat = jsonFormat11(JsonSpec.apply)

    /** Read from a String.
      * @param s a String to read.
      * @return the result
      */
    override def fromString(s: String) = s.parseJson.convertTo[JsonSpec]
}
