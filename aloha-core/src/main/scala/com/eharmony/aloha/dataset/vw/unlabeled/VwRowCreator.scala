package com.eharmony.aloha.dataset.vw.unlabeled

import java.text.DecimalFormat

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.vw.VwCovariateProducer
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator.{DefaultVwNamespaceName, inEpsilonInterval}
import com.eharmony.aloha.dataset.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.aloha.dataset._
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.util.Logging
import spray.json._

import scala.annotation.tailrec
import scala.collection.immutable.BitSet
import scala.util.Try


/**
 * A Spec to create VW input.
 * @param featuresFunction function to create covariate data.
 * @param defaultNamespace indices of features.  This is where features with no associated namespace are pigeonholed.
 * @param namespaces index mapping from namespace name to feature index.
 * @param normalizer a function that can alter the output.
 * @param includeZeroValues whether to include key-value pairs in VW input whose the values are equal to zero.
 * @tparam A input type from which features is extracted.
 */
@throws[IllegalArgumentException]("If any value {0, 1, ..., featuresFunction.features.size - 1} is not mapped to defaultNamespace or namespaces.")
class VwRowCreator[-A](
        val featuresFunction: FeatureExtractorFunction[A, Sparse],
        val defaultNamespace: List[Int],
        val namespaces: List[(String, List[Int])],
        val normalizer: Option[CharSequence => CharSequence],
        val includeZeroValues: Boolean = false)
extends RowCreator[A]
   with java.io.Serializable
   with Logging {

    {
        val req = BitSet(featuresFunction.features.indices:_*)
        val act = (BitSet(defaultNamespace:_*) /: namespaces)(_ ++ _._2)
        require(req == act, s"defaultNamespace and namespaces must cover all indices (0 until ${featuresFunction.features.size}).  Missing ${(req -- act).mkString(",")}")
    }

    private[this] val nonEmptyNamespaces = namespaces.filter(_._2.nonEmpty)

    // Log the empty namespaces.
    Option(namespaces.unzip._1.toSet -- nonEmptyNamespaces.unzip._1.toSet).
        collect { case s if s.nonEmpty => s.toVector.sorted.mkString(", ") }.
        foreach { empty => info(s"The following namespaces were empty: $empty.") }


    def apply(data: A): (MissingAndErroneousFeatureInfo, CharSequence) = {
        val (extractionInfo, features) = featuresFunction(data)
        val vwIn = unlabeledVwInput(features)
        (extractionInfo, vwIn)
    }

    /**
     * Each namespace will only be present if it contains at least one feature in the namespace.
     * @param features features to insert into VW input line.
     * @return
     */
    def unlabeledVwInput(features: IndexedSeq[Sparse]) = {

        // RMD 2015-06-12: GOD I HATE THIS CODE!!!  Maybe functionalize it in the future!

        val sb = new StringBuilder

        // Whether a namespace has been added previously.
        var nsAlreadyInserted = false

        if (defaultNamespace.nonEmpty)
            nsAlreadyInserted = addNamespaceFeaturesToVwLine(sb, nsAlreadyInserted, DefaultVwNamespaceName, defaultNamespace, features, includeZeroValues = includeZeroValues)

        var nss: List[(String, List[Int])] = nonEmptyNamespaces
        while (nss.nonEmpty) {
            val ns = nss.head
            nsAlreadyInserted = addNamespaceFeaturesToVwLine(sb, nsAlreadyInserted, ns._1, ns._2, features, includeZeroValues)
            nss = nss.tail
        }

        // If necessary, apply the normalizer.
        val vwLine = normalizer.map(n => n(sb)).getOrElse(sb)
        vwLine
    }

    /**
     * Add data from a namespace to the VW line.  Data comes in the form of an iterable sequence of key-value pairs
     * where keys are strings and values are doubles.  The values are truncated according to
     * [[VwRowCreator.DecimalFormatter]].  If the truncated value is the integer, 1, then the value is omitted from the
     * output (as is allowed by VW).  If the truncated value is zero, then the feature is included only if
     * ''includeZeroValues'' is true.
     *
     * @param sb string builder into which data is
     * @param previousNsInserted Whether a namespace has previously been inserted
     * @param nsName the namespace name.
     * @param nsFeatureIndices the feature indices included in the referenced namespace.
     * @param features the entire list of features (across all namespaces).  Since this is an ''IndexedSeq'', lookup
     *                 by index is constant or near-constant time.
     * @param includeZeroValues whether to include key-value pairs in the VW output whose values are zero.
     * @return
     */
    private[this] def addNamespaceFeaturesToVwLine(
            sb: StringBuilder,
            previousNsInserted: Boolean,
            nsName: String,
            nsFeatureIndices: List[Int],
            features: IndexedSeq[Sparse],
            includeZeroValues: Boolean): Boolean = {

        @tailrec def h(indices: List[Int], inserted: Boolean, nameAlreadyInserted: Boolean): Boolean = {
            if (indices.isEmpty) inserted
            else {
                val baseIt = features(indices.head).iterator
                val it = if (includeZeroValues) baseIt else baseIt.filter(f => !inEpsilonInterval(f._2))

                val nameIns = nameAlreadyInserted || it.hasNext
                val ins = inserted || it.hasNext

                if (!nameAlreadyInserted && it.hasNext) {
                    if (inserted) sb.append(" ")
                    sb.append("|").append(nsName)
                }

                while(it.hasNext) {
                    val (feature, value) = it.next()
                        if (VwRowCreator.inEpsilonInterval(value - 1)) {
                            if (ins) sb.append(" ")
                            sb.append(feature)
                        }
                        else if (!inEpsilonInterval(value) || includeZeroValues) {
                            // For double values, format it to 6 decimals.  VW seems to not handle crazy long
                            // numbers too well.  Note that for super large numbers, this DecimalFormat will
                            // spit out very large strings of numbers. i don't think very large weights occur
                            // that often with VW so for now i'm not addressing that (potential) issue.
                            if (ins) sb.append(" ")
                            sb.append(feature).append(":").append(VwRowCreator.DecimalFormatter.format(value))
                        }
                }

                h(indices.tail, ins, nameIns)
            }
        }

        h(nsFeatureIndices, previousNsInserted, nameAlreadyInserted = false)
    }
}

final object VwRowCreator {

    private[vw] val DefaultVwNamespaceName = ""
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

    final class Producer[A]
        extends RowCreatorProducer[A, VwRowCreator[A]]
        with RowCreatorProducerName
        with VwCovariateProducer[A]
        with SparseCovariateProducer
        with CompilerFailureMessages {

        type JsonType = VwUnlabeledJson
        def parse(json: JsValue): Try[VwUnlabeledJson] = Try { json.convertTo[VwUnlabeledJson] }
        def getRowCreator(semantics: CompiledSemantics[A], jsonSpec: VwUnlabeledJson): Try[VwRowCreator[A]] = {
            val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)
            val spec = covariates.map(c => new VwRowCreator(c, default, nss, normalizer))
            spec
        }
    }
}
