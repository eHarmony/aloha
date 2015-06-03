package com.eharmony.matching.aloha.models.vw.jni

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}

import java.text.DecimalFormat

import spray.json.{DeserializationException, JsValue, JsonReader}

import vw.VWScorer

import com.eharmony.matching.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.models.reg.RegressionFeatures
import com.eharmony.matching.aloha.models.{BaseModel, TypeCoercion}
import com.eharmony.matching.aloha.reflect._
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.{EitherHelpers, Logging}


/**
 * Model that delegates to a VW JNI model.
 * @param modelId a modelId
 * @param model VW JNI instance
 * @param featureNames names of features (parallel to featureFunctions)
 * @param featureFunctions functions to extract values from the input value
 * @param defaultNs indices of features that will be placed in the default VW namespace
 * @param namespaces mapping from namespace name to indices of features that will be placed in the namespace
 * @param finalizer a function that transforms the native VW output type (Float) to B
 * @param numMissingThreshold A threshold dictating how many missing features to allow before making the
 *                            prediction fail.  See [[com.eharmony.matching.aloha.models.reg.RegressionFeatures.numMissingThreshold]]
 * @param scb a score converter
 * @tparam A model input type
 * @tparam B model output type
 * @author R M Deak
 */
final case class VwJniModel[-A, +B](
    modelId: ModelIdentity,
    private val model: vw.VWScorer,
    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    finalizer: Float => B,
    numMissingThreshold: Option[Int] = None)(implicit private[this] val scb: ScoreConverter[B])
extends BaseModel[A, B]
   with RegressionFeatures[A]
   with Logging {

    {
        require(
            featureNames.size == featureFunctions.size,
            s"featureNames.size (${featureNames.size}}) != featureFunctions.size (${featureFunctions.size}})")

        val req = sci.BitSet(featureFunctions.indices:_*)
        val act = (sci.BitSet(defaultNs:_*) /: namespaces)(_ ++ _._2)
        require(
            req == act,
            s"defaultNamespace and namespaces must cover all indices (0 until ${featureFunctions.size}).  Missing ${(req -- act).mkString(",")}")
    }

    import VwJniModel._


    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
        generateVwInput(a) match {
            case Left(missing) => failure(Seq(s"Too many features with missing variables: ${missing.count(_._2.nonEmpty)}"), getMissingVariables(missing))
            case Right(vwIn) =>
                Try { model.getPrediction(vwIn) } match {
                    case Success(y) => success(finalizer(y))
                    case Failure(ex) => failure(Seq(ex.getMessage))
                }
        }
    }

    // TODO: Figure out how to make the missing features fast.
    def getMissingVariables(missing: scm.Map[String, Seq[String]]): Seq[String] =
        missing.unzip._2.foldLeft(Set.empty[String])(_ ++ _).toIndexedSeq.sorted

    /**
     * Get a VW line (on the right) or a map of missing features (on the left) if there was too much
     * missing data to form a prediction.
     * @param a input
     * @return a result
     */
    private[jni] def generateVwInput(a: A): Either[scm.Map[String, Seq[String]], String] = {
        val Features(features, missing, missingOk) = constructFeatures(a)

        if (!missingOk) Left(missing)
        else {
            val b = new StringBuilder

            if (defaultNs.nonEmpty) {
                b.append("| ")
                defaultNs foreach { i =>
                    features(i) foreach { case (f, v) =>
                        if (!inEpsilonInterval(v)) {
                            b.append(f)
                            if (!inEpsilonInterval(v - 1)) b.append(":").append(DecimalFormatter.format(v))
                            b.append(" ")
                        }
                    }
                }
            }

            namespaces foreach { case(ns, ind) =>
                b.append("|").append(ns).append(" ")
                ind foreach { i =>
                    features(i) foreach { case (f, v) =>
                        if (!inEpsilonInterval(v)) {
                            b.append(f)
                            if (!inEpsilonInterval(v - 1)) b.append(":").append(DecimalFormatter.format(v))
                            b.append(" ")
                        }
                    }
                }
            }

            Right(b.toString())
        }
    }
}

object VwJniModel extends ParserProviderCompanion with VwJniModelJson {

    // Copied from featureSpecExtractor.vw.unlabeled.VwSpec object.

    private[jni] val FeatureDecimalDigits = 6
    private[jni] val DecimalFormatter = new DecimalFormat(List.fill(FeatureDecimalDigits)("#").mkString("0.", "", ""))
    private[this] val eps = math.pow(10, -FeatureDecimalDigits) / 2
    private[this] val negEps = -eps
    private[jni] def inEpsilonInterval(x: Double) = negEps < x && x < eps

    object Parser extends ModelParserWithSemantics with EitherHelpers { self =>
        val modelType = "VwJNI"

        override def modelJsonReader[A, B: JsonReader: ScoreConverter](semantics: Semantics[A]): JsonReader[VwJniModel[A, B]] = new JsonReader[VwJniModel[A, B]] {
            override def read(json: JsValue): VwJniModel[A, B] = {
                val vw = json.convertTo[VwJNIAst]

                features(vw.features.toSeq, semantics) match {
                    case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
                    case Right(featureMap) =>
                        val (names, functions) = featureMap.toIndexedSeq.unzip

                        // Conversion function.
                        val cf = {
                            implicit val riB = implicitly[ScoreConverter[B]].ri
                            TypeCoercion[Float, B] getOrElse {
                                throw new DeserializationException(s"Couldn't find conversion function to ${RefInfoOps.toString[B]}")
                            }
                        }

                        // If VW params are unspecified, use empty string.
                        val vwParams = vw.vw.getParams.fold(_.mkString(" "), identity)
                        val vwJniModel = new VWScorer(vwParams)
                        val indices = featureMap.unzip._1.view.zipWithIndex.toMap
                        val nssRaw = vw.namespaces.getOrElse(sci.ListMap.empty)
                        val nss = nssRaw.map { case (ns, fs) => (ns, fs.flatMap(indices.get).toList)}.toList
                        val defaultNs = (indices.keySet -- (Set.empty[String] /: nssRaw)(_ ++ _._2)).flatMap(indices.get).toList
                        VwJniModel(vw.modelId, vwJniModel, names, functions, defaultNs, nss, cf, vw.numMissingThreshold)
                }
            }
        }

        // TODO: Copied from RegressionModel.  Refactor for reuse.
        private[this] def features[A](featureMap: Seq[(String, Spec)], semantics: Semantics[A]) =
            mapSeq(featureMap) {
                case (k, Spec(spec, default)) =>
                    semantics.createFunction[Iterable[(String, Double)]](spec, default).
                        left.map { Seq(s"Error processing spec '$spec'") ++ _ }. // Add the spec that errored.
                        right.map { f => (k, f) }
            }
    }

    override def parser: ModelParser = Parser
}
