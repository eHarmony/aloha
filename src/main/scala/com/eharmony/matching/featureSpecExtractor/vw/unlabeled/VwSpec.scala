package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import java.text.DecimalFormat

import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.{FeatureExtractorFunction, MissingAndErroneousFeatureInfo, Spec}

import scala.collection.{immutable => sci}


class VwSpec[A](
        val featuresFunction: FeatureExtractorFunction[A, Sparse],
        val defaultNamespace: sci.IndexedSeq[Int],
        val namespaces: sci.IndexedSeq[(String, sci.IndexedSeq[Int])],
        val normalizer: Option[CharSequence => String])
extends Spec[A]
with java.io.Serializable {


    def toInput(data: A) = toInput(data, includeZeroValues = false)

    def toInput(data: A, includeZeroValues: Boolean) = toInput(data, includeZeroValues, new StringBuilder)

    protected[this] def toInput(data: A, includeZeroValues: Boolean, sb: StringBuilder): (MissingAndErroneousFeatureInfo, String) = {
        val (extractionInfo, features) = featuresFunction(data)
        if (defaultNamespace.nonEmpty)
            addValuesToVwLine(sb, defaultNamespace, features, includeZeroValues)

        namespaces.foreach { ns =>
            addValuesToVwLine(sb.append(" |").append(ns._1), ns._2, features, includeZeroValues)
        }

        // If necessary, apply the normalizer.
        val vwLine = normalizer.map(n => n(sb)).getOrElse(sb.toString())
        (extractionInfo, vwLine)
    }

    private[this] def addValuesToVwLine(
            sb: StringBuilder,
            indices: Seq[Int],
            features: IndexedSeq[Sparse],
            includeZeroValues: Boolean) {

        indices.foreach { i =>
            sb.append(" ")
            val it = features(i).iterator
            while(it.hasNext) {
                val value = it.next()

                // if the value is an integer then drop the decimals
                // if the value == 1 then drop the value since that's vw's default
                // if the value == 0 then drop the entire feature

                // In order to do a bag of words model on some free text, we need to
                // have some feature names that are just spaces.  We want to clean
                // that up here, by replacing early spaces with nothing to make
                // the VW input better formatted.

                val feature = value._1.replaceAll("^\\s+", "")
                val intVal = value._2.toInt

                if (intVal == value._2) {
                    if (1 == intVal)
                        sb.append(feature)
                    else if (includeZeroValues || intVal != 0)
                        sb.append(feature).append(":").append(intVal)
                }
                else {
                    // for double values, format it to 6 decimals...VW seems to not handle crazy long
                    // numbers too well. note that for super large numbers, this DECIMAL_FORMATTER
                    // will spit out very large strings of numbers. i don't think very large weights occur that often with VW so
                    // for now i'm not addressing that (potential) issue.
                    sb.append(feature).append(":").append(VwSpec.DecimalFormatter.format(value._2))
                }

                if (it.hasNext)
                    sb.append(" ")
            }
        }
    }
}

object VwSpec {
    private val DecimalFormatter = new DecimalFormat("0.######")
}
