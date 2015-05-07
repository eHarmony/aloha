package com.eharmony.matching.featureSpecExtractor.vw.cb

import scala.collection.{immutable => sci}

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec
import com.eharmony.matching.featureSpecExtractor.FeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.density.Sparse

class VwContextualBanditSpec[A](
        featuresFunction: FeatureExtractorFunction[A, Sparse],
        defaultNamespace: sci.IndexedSeq[Int],
        namespaces: sci.IndexedSeq[(String, sci.IndexedSeq[Int])],
        normalizer: Option[CharSequence => CharSequence],
        cbAction: GenAggFunc[A, String],
        cbCost: GenAggFunc[A, String],
        cbProbability: GenAggFunc[A, String],
        includeZeroValues: Boolean = false)
extends VwSpec[A](featuresFunction, defaultNamespace, namespaces, normalizer, includeZeroValues)
with java.io.Serializable  {

    override def apply(data: A) = {
        val actionVal = cbAction(data)
        val costVal = cbCost(data)
        val probabilityVal = cbProbability(data)
        val (missing, iv) = super.apply(data)
        val sb = new StringBuilder().
                 append(actionVal).append(":").
                 append(costVal).append(":").
                 append(probabilityVal).append("|").
                 append(iv)

        (missing, sb)
    }
}
