package com.eharmony.matching.featureSpecExtractor.vw.unlabeled.json

import scala.collection.{immutable => sci}
import com.eharmony.matching.featureSpecExtractor.json.{Namespace, SparseSpec}


case class VwUnlabeledJson(
        imports: Seq[String],
        features: sci.IndexedSeq[SparseSpec],
        namespaces: Option[Seq[Namespace]] = None,
        normalizeFeatures: Option[Boolean] = Some(false))
extends VwUnlabeledJsonLike

