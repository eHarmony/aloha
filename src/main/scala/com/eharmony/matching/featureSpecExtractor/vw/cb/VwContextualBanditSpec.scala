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
        normalizer: Option[CharSequence => String],
        cbAction: GenAggFunc[A, String],
        cbCost: GenAggFunc[A, String],
        cbProbability: GenAggFunc[A, String])
extends VwSpec[A](featuresFunction, defaultNamespace, namespaces, normalizer)
with java.io.Serializable  {

    override def toInput(data: A, includeZeroValues: Boolean) = {
        val actionVal = cbAction(data)
        val costVal = cbCost(data)
        val probabilityVal = cbProbability(data)
        val sb = new StringBuilder().append(actionVal).append(":").append(costVal).append(":").append(probabilityVal)
        toInput(data, includeZeroValues, sb)
    }
}
