package com.eharmony.matching.aloha.models.vw.jni


import java.text.DecimalFormat

import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.models.BaseModel
import com.eharmony.matching.aloha.models.reg.RegressionFeatures
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.Logging

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}


final case class VwJniModel[-A, +B: ScoreConverter](
    private val model: vw.VWScorer,
    modelId: ModelIdentity,
    features: sci.IndexedSeq[(String, GenAggFunc[A, Iterable[(String, Double)]])],
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    finalizer: Float => B,
    numMissingThreshold: Option[Int] = None)
extends BaseModel[A, B]
   with RegressionFeatures[A]
   with Logging {

    import VwJniModel._

    protected[this] override val (featureNames, featureFunctions) = features.unzip

    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
    override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
        generateVwInput(a) match {
            case Left(missing) => failure(Seq(s"Too many features with missing variables: ${missing.size}"), getMissingVariables(missing))
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
                            if (inEpsilonInterval(v - 1)) b.append("=").append(DecimalFormatter.format(v))
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
                            if (inEpsilonInterval(v - 1)) b.append("=").append(DecimalFormatter.format(v))
                            b.append(" ")
                        }
                    }
                }
            }

            Right(b.toString())
        }
    }
}

object VwJniModel {
    private[jni] val FeatureDecimalDigits = 6
    private[jni] val DecimalFormatter = new DecimalFormat(List.fill(FeatureDecimalDigits)("#").mkString("0.", "", ""))
    private[this] val eps = math.pow(10, -FeatureDecimalDigits) / 2
    private[this] val negEps = -eps
    private[jni] def inEpsilonInterval(x: Double) = negEps < x && x < eps
}
