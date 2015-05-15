package com.eharmony.matching.featureSpecExtractor.vw.labeled

import com.eharmony.matching.aloha.semantics.func.{GenAggFunc, GenFunc0}
import com.eharmony.matching.featureSpecExtractor.{LabelSpec, FeatureExtractorFunction}
import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec

import scala.collection.{immutable => sci}

class VwLabelSpec[A](
        featuresFunction: FeatureExtractorFunction[A, Sparse],
        defaultNamespace: sci.IndexedSeq[Int],
        namespaces: sci.IndexedSeq[(String, sci.IndexedSeq[Int])],
        normalizer: Option[CharSequence => CharSequence],
        val label: GenAggFunc[A, String],
        importance: Option[GenAggFunc[A, String]],
        includeZeroValues: Boolean = false)
extends VwSpec[A](featuresFunction, defaultNamespace, namespaces, normalizer, includeZeroValues)
   with LabelSpec[A] {

    private[this] val _importance = importance.getOrElse(GenFunc0[Any, String]("1", _ => "1"))

    def getLabelValue(data: A): String = label(data)

    override def apply(data: A) = {
        val labelVal = getLabelValue(data)
        val i = _importance(data)
        val (missing, iv) = super.apply(data)
        val sb = new StringBuilder().append(labelVal).append(" ")

        val out = (if (i != "1" && i != "1.0")
                       sb.append(i).append(" ")
                   else sb
                  ).
                  append(labelVal).
                  append("|").
                  append(iv)

        (missing, out)
    }
}
