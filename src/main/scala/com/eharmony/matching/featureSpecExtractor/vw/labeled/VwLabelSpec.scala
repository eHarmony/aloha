package com.eharmony.matching.featureSpecExtractor.vw.labeled

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.FeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec

import scala.collection.{immutable => sci}

class VwLabelSpec[A](
        featuresFunction: FeatureExtractorFunction[A],
        defaultNamespace: sci.IndexedSeq[Int],
        namespaces: sci.IndexedSeq[(String, sci.IndexedSeq[Int])],
        normalizer: Option[CharSequence => String],
        label: GenAggFunc[A, String],
        importance: Option[GenAggFunc[A, String]]
) extends VwSpec[A](featuresFunction, defaultNamespace, namespaces, normalizer) {

    def getLabelValue(data: A): String = label(data)

    override def toInput(data: A, includeZeroValues: Boolean) = {
        val labelVal = getLabelValue(data)
        val sb = (new StringBuilder).append(labelVal).append(" ")
        val sbImp = importance.fold(sb){i =>
            val imp = i(data)
            if (imp != "1" && imp != "1.0")
                sb.append(imp).append(" ")
            else sb
        }

        // add label as tag so that we can see it when we get a predictions file from vw
        val sbLab = sbImp.append(labelVal).append("|")

        println(sbLab.toString())

        toInput(data, includeZeroValues, sbLab)
    }
}
