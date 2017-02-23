package com.eharmony.aloha.dataset.vw.json

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.json.validation.{FeatureValidation, NsValidation, Validation}
import com.eharmony.aloha.dataset.json.{CovariateJson, Namespace}

trait VwJsonLike
extends CovariateJson[Sparse]
   with Validation
   with FeatureValidation[Sparse]
   with NsValidation {

    val namespaces: Option[Seq[Namespace]]
    val normalizeFeatures: Option[Boolean]

    def validate() = validateFeatureNames orElse validateNsNames orElse validateNsFeatures

    /**
     * Get the default namespace index mapping and the mapping from each namespace name to the feature index.
     * @return
     */
    final def namespaceIndices(): (List[Int], List[(String, List[Int])]) = {
        // Mapping from feature name to feature index.
        val fMap = features.view.zipWithIndex.map{case(k, v) => (k.name, v)}.toMap

        // Mapping from namespace name to sequence of feature indices.
        val nss = namespaces.getOrElse(Seq.empty).map(ns => (ns.name, ns.features.flatMap(fMap.get).toList)).toList

        // default (unnamed) namespace mapping.  These are the indices not in any namespace.  Sorted.
        val default = nss.foldLeft(features.indices.toSet)((ind, ns) => ind -- ns._2).toList.sorted
        (default, nss)
    }

    def shouldNormalizeFeatures: Boolean = normalizeFeatures.getOrElse(false)
}
