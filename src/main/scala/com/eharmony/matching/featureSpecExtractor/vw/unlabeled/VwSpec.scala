package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import java.text.DecimalFormat

import com.eharmony.matching.featureSpecExtractor.density.Sparse
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec.inEpsilonInterval
import com.eharmony.matching.featureSpecExtractor.{MissingAndErroneousFeatureInfo, FeatureExtractorFunction, Spec}

import scala.collection.{immutable => sci, BitSet}

/**
 * A Spec to create VW input.
 * @param featuresFunction function to create covariate data.
 * @param defaultNamespace indices of features.  This is where features with no associated namespace are pigeonholed.
 * @param namespaces index mapping from namespace name to feature index.
 * @param normalizer
 * @param includeZeroValues whether to include key-value pairs in VW input whose the values are equal to zero.
 * @tparam A input type from which features is extracted.
 */
@throws[IllegalArgumentException]("If any value {0, 1, ..., featuresFunction.features.size - 1} is not mapped to defaultNamespace or namespaces.")
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
        require(req == act, s"defaultNamespace and namespaces must cover all indices (0 until ${featuresFunction.features.size}).  Missing ${(req -- act).mkString(",")}")
    }

    def apply(data: A): (MissingAndErroneousFeatureInfo, CharSequence) = {
        val sb = new StringBuilder

        val (extractionInfo, features) = featuresFunction(data)
        if (defaultNamespace.nonEmpty)
            addNamespaceFeaturesToVwLine(sb, defaultNamespace, features, includeZeroValues)

        namespaces.foreach { ns =>
            addNamespaceFeaturesToVwLine(sb.append(" |").append(ns._1), ns._2, features, includeZeroValues)
        }

        // If necessary, apply the normalizer.
        val vwLine = normalizer.map(n => n(sb)).getOrElse(sb)
        (extractionInfo, vwLine)
    }

    /**
     * Add data from a namespace to the VW line.  Data comes in the form of an iterable sequence of key-value pairs
     * where keys are strings and values are doubles.  The values are truncated according to
     * [[VwSpec.DecimalFormatter]].  If the truncated value is the integer, 1, then the value is omitted from the
     * output (as is allowed by VW).  If the truncated value is zero, then the feature is included only if
     * ''includeZeroValues'' is true.
     * @param sb string builder into which data is
     * @param nsFeatureIndices the feature indices included in the referenced namespace.
     * @param features the entire list of features (across all namespaces).  Since this is an ''IndexedSeq'', lookup
     *                 by index is constant or near-constant time.
     * @param includeZeroValues whether to include key-value pairs in the VW output whose values are zero.
     */
    private[this] def addNamespaceFeaturesToVwLine(
            sb: StringBuilder,
            nsFeatureIndices: sci.IndexedSeq[Int],
            features: IndexedSeq[Sparse],
            includeZeroValues: Boolean) {

        nsFeatureIndices.foreach { i =>
            sb.append(" ")
            val it = features(i).iterator
            while(it.hasNext) {
                val (feature, value) = it.next()

                if (VwSpec.inEpsilonInterval(value - 1)) {
                    sb.append(feature)
                    if (it.hasNext) sb.append(" ")
                }
                else if (!inEpsilonInterval(value) || includeZeroValues) {
                    // For double values, format it to 6 decimals.  VW seems to not handle crazy long
                    // numbers too well.  Note that for super large numbers, this DecimalFormat will
                    // spit out very large strings of numbers. i don't think very large weights occur
                    // that often with VW so for now i'm not addressing that (potential) issue.
                    sb.append(feature).append(":").append(VwSpec.DecimalFormatter.format(value))
                    if (it.hasNext) sb.append(" ")
                }
            }
        }
    }
}

object VwSpec {
    /**
     * The reason to choose 17 digits is that
     1 1 == (1 - 1.0e-17)
     1 0.9999999999999999 == (1 - 1.0e-16).
     * We want to retain as much information as possible without allowing long trailing sequences of zeroes.
     */
    private[vw] val LabelDecimalDigits = 17
    private[vw] val LabelDecimalFormatter = new DecimalFormat(List.fill(LabelDecimalDigits)("#").mkString("0.", "", ""))
    private[this] val labelEps = math.pow(10, -LabelDecimalDigits) / 2
    private[this] val labelNegEps = -labelEps
    private[vw] def labelInEpsilonInterval(label: Double) = labelNegEps < label && label < labelEps

    private[vw] val FeatureDecimalDigits = 6
    private[vw] val DecimalFormatter = new DecimalFormat(List.fill(FeatureDecimalDigits)("#").mkString("0.", "", ""))
    private[this] val eps = math.pow(10, -FeatureDecimalDigits) / 2
    private[this] val negEps = -eps
    private[vw] def inEpsilonInterval(x: Double) = negEps < x && x < eps
}
