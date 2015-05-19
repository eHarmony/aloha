package com.eharmony.matching.featureSpecExtractor.vw.cb

import com.eharmony.matching.aloha.util.Logging

import scala.collection.{immutable => sci}

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec
import com.eharmony.matching.featureSpecExtractor.FeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.density.Sparse

final case class VwContextualBanditSpec[A](
        override val featuresFunction: FeatureExtractorFunction[A, Sparse],
        override val defaultNamespace: sci.IndexedSeq[Int],
        override val namespaces: sci.IndexedSeq[(String, sci.IndexedSeq[Int])],
        override val normalizer: Option[CharSequence => CharSequence],
        cbAction: GenAggFunc[A, Option[Long]],
        cbCost: GenAggFunc[A, Option[Double]],
        cbProbability: GenAggFunc[A, Option[Double]],
        override val includeZeroValues: Boolean = false)
extends VwSpec[A](featuresFunction, defaultNamespace, namespaces, normalizer, includeZeroValues)
   with Logging
   with java.io.Serializable  {

    override def apply(data: A) = {
        val (missing, iv) = super.apply(data)

        val lineOpt = for {
            action <- cbAction(data)
            cost <- cbCost(data)
            prob <- cbProbability(data)
        } yield {
            new StringBuilder().
                append(action).append(":").
                append(cost).append(":").
                append(prob).append("|").
                append(iv)
        }

        if (lineOpt.isEmpty) debug("Contextual Bandit label information is missing. Creating a line with no label.")

        val line = lineOpt.getOrElse(iv)
        (missing, line)
    }
}
