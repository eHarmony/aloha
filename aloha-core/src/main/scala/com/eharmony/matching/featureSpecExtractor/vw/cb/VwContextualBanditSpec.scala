package com.eharmony.matching.featureSpecExtractor.vw.cb

import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.Logging
import com.eharmony.matching.featureSpecExtractor.FeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec

import scala.collection.{immutable => sci}

final case class VwContextualBanditSpec[-A](
        override val featuresFunction: FeatureExtractorFunction[A, Sparse],
        override val defaultNamespace: List[Int],
        override val namespaces: List[(String, List[Int])],
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
            a <- action(data)
            c <- cost(data)
            p <- probability(data)
        } yield {
            new StringBuilder().
                append(a).append(":").
                append(VwSpec.LabelDecimalFormatter.format(c)).append(":").
                append(VwSpec.LabelDecimalFormatter.format(p)).
                append(if (0 == iv.length()) "|" else iv)

//                append("|").
//                append(iv)
        }

        if (lineOpt.isEmpty) debug("Contextual Bandit label information is missing. Creating a line with no label.")

        val line = lineOpt.getOrElse(iv)
        (missing, line)
    }

    private[this] def action(data: A): Option[Long] = cbAction(data).filter(_ > 0)
    private[this] def cost(data: A): Option[Double] = cbCost(data)
    private[this] def probability(data: A): Option[Double] = cbProbability(data).filter(p => 0 <= p && p <= 1)
}
