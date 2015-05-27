package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import java.text.DecimalFormat

import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec.inEpsilonInterval
import com.eharmony.matching.featureSpecExtractor.{MissingAndErroneousFeatureInfo, FeatureExtractorFunction, Spec}

import scala.collection.{immutable => sci, BitSet}


class VwSpec[-A](
        val featuresFunction: FeatureExtractorFunction[A, Sparse],
        val defaultNamespace: sci.IndexedSeq[Int],
        val namespaces: sci.IndexedSeq[(String, sci.IndexedSeq[Int])],
        val normalizer: Option[CharSequence => CharSequence],
        val includeZeroValues: Boolean = false)
extends Spec[A]
with java.io.Serializable {
    {
        val req = BitSet(0 until featuresFunction.features.size:_*)
        val act = (BitSet(defaultNamespace:_*) /: namespaces)(_ ++ _._2)
        require(req == act, s"defaultNamespace and namespaces must cover all indices (0 until ${featuresFunction.features.size - 1}).  Missing ${(req -- act).mkString(",")}")
    }

    def apply(data: A): (MissingAndErroneousFeatureInfo, CharSequence) = {
        val sb = new StringBuilder

        val (extractionInfo, features) = featuresFunction(data)
        if (defaultNamespace.nonEmpty)
            addValuesToVwLine(sb, defaultNamespace, features, includeZeroValues)

        namespaces.foreach { ns =>
            addValuesToVwLine(sb.append(" |").append(ns._1), ns._2, features, includeZeroValues)
        }

        // If necessary, apply the normalizer.
        val vwLine = normalizer.map(n => n(sb)).getOrElse(sb)
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
                val fv = value._2

                if (1 == fv) {
                    sb.append(feature)
                    if (it.hasNext) sb.append(" ")
                }
                else if (!inEpsilonInterval(fv) || includeZeroValues) {
                    // For double values, format it to 6 decimals.  VW seems to not handle crazy long
                    // numbers too well.  Note that for super large numbers, this DecimalFormat will
                    // spit out very large strings of numbers. i don't think very large weights occur
                    // that often with VW so for now i'm not addressing that (potential) issue.
                    sb.append(feature).append(":").append(VwSpec.DecimalFormatter.format(fv))
                    if (it.hasNext) sb.append(" ")
                }

//                val intVal = value._2.toInt
//
//                if (intVal == value._2) {
//                    if (1 == intVal)
//                        sb.append(feature)
//                    else if (includeZeroValues || intVal != 0)
//                        sb.append(feature).append(":").append(intVal)
//                }
//                else {
//                    // for double values, format it to 6 decimals...VW seems to not handle crazy long
//                    // numbers too well. note that for super large numbers, this DECIMAL_FORMATTER
//                    // will spit out very large strings of numbers. i don't think very large weights occur that often with VW so
//                    // for now i'm not addressing that (potential) issue.
//                    sb.append(feature).append(":").append(VwSpec.DecimalFormatter.format(value._2))
//                }
//
//                if (it.hasNext)
//                    sb.append(" ")
            }
        }
    }
}

object VwSpec {
    /**
     * The reason to choose 17 digits is that
     1 0 == (1 - 1.0e-17)
     1 0.9999999999999999 == (1 - 1.0e-16).
     * We want to retain as much information as possible without allowing long trailing sequences of zeroes.
     */
    private[this] val LabelDecimalDigits = 17
    private[vw] val LabelDecimalFormatter = new DecimalFormat(List.fill(LabelDecimalDigits)("#").mkString("0.", "", ""))
    private[this] val labelEps = math.pow(10, -LabelDecimalDigits)
    private[this] val labelNegEps = -labelEps
    private[vw] def labelInEpsilonInterval(label: Double) = labelNegEps < label && label < labelEps


    private[this] val FeatureDecimalDigits = 6
    private[vw] val DecimalFormatter = new DecimalFormat(List.fill(FeatureDecimalDigits)("#").mkString("0.", "", ""))
    private[this] val eps = math.pow(10, -FeatureDecimalDigits)
    private[this] val negEps = -eps
    private[vw] def inEpsilonInterval(x: Double) = negEps < x && x < eps
}
