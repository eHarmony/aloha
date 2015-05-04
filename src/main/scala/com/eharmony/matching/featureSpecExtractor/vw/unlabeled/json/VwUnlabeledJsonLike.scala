package com.eharmony.matching.featureSpecExtractor.vw.unlabeled.json

import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.json.{CovariateJson, Namespace, SparseSpec}

import scala.collection.{immutable => sci, SeqView}

trait VwUnlabeledJsonLike
extends CovariateJson[Sparse] {

    val imports: Seq[String]
    val features: sci.IndexedSeq[SparseSpec]
    val namespaces: Option[Seq[Namespace]]
    val normalizeFeatures: Option[Boolean]

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

