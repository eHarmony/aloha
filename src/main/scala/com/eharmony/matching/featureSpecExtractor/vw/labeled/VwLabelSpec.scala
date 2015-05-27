package com.eharmony.matching.featureSpecExtractor.vw.labeled

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.Logging
import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec
import com.eharmony.matching.featureSpecExtractor.{FeatureExtractorFunction, LabelSpec}

import scala.collection.{immutable => sci}

final case class VwLabelSpec[-A](
        override val featuresFunction: FeatureExtractorFunction[A, Sparse],
        override val defaultNamespace: sci.IndexedSeq[Int],
        override val namespaces: sci.IndexedSeq[(String, sci.IndexedSeq[Int])],
        override val normalizer: Option[CharSequence => CharSequence],
        label: GenAggFunc[A, Option[Double]],
        importance: GenAggFunc[A, Option[Double]],
        tag: GenAggFunc[A, Option[String]],
        override val includeZeroValues: Boolean = false)
extends VwSpec[A](featuresFunction, defaultNamespace, namespaces, normalizer, includeZeroValues)
   with LabelSpec[A]
   with Logging {
    override def apply(data: A) = {
        val (missing, iv) = super.apply(data)

        // If importance or label is missing, this will return None.
        // Otherwise, It'll be the entire line with dep vars and indep vars.
        val lineOpt = for {
            imp <- importance(data)
            lab <- label(data)
            t = tag(data).getOrElse("").trim

        } yield {
            val sb = new StringBuilder().
                append(VwSpec.LabelDecimalFormatter.format(lab)).  // VW input format [Label].
                append(" ")
            (if (imp == 1) sb
             else sb.
                append(VwSpec.LabelDecimalFormatter.format(imp)).  // VW input format [Importance].
                append(" ")
            ).append(t).     // VW input format [Tag].
              append("|").   // There must be NO leading space b/c the tag must touch the pipe.
              append(iv)     // Features.
        }

        if (lineOpt.isEmpty) debug("Label information is missing. Creating a line with no label.")

        // If label information is missing, just return the indep vars.  This will cause vw
        // to ignore the line for training.
        val line = lineOpt.getOrElse(iv)
        (missing, line)
    }

    override val stringLabel = label.andThenGenAggFunc(labOpt => labOpt.map(lab => VwSpec.LabelDecimalFormatter.format(lab.toString)))
}
