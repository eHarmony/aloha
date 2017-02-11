package com.eharmony.aloha.models.reg

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.reg.json.{RegressionModelJson, Spec}
import com.eharmony.aloha.models.{SubmodelBase, Subvalue}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, Logging}

import scala.collection.{immutable => sci}
import java.{lang => jl}

/**
 * A regression model capable of doing not only linear regression but polynomial regression in general.
 *
 * {{{
 * val regImp = "com.eharmony.aloha.models.reg.RegressionModelValueToTupleConversions._"
 * val compiler = ...
 * val plugin = ...
 * val imports: Seq[String] = ...
 * val s = CompiledSemantics(compiler, plugin, imports :+ regImp)
 * }}}
 *
 * This is useful because these conversions allow implicit conversion function from some of the AnyVal types and
 * Options of AnyVal types to Iterable[(String, Double)].  This is useful because specifying features in the JSON
 * spec like:
 * {{{
 * {
 *   ...
 *   "features": {
 *     "intercept": "-3",
 *     "income": "${user.profile.income}"
 *   }
 * }
 * }}}
 *
 * into sequences like:
 *
 * {{{
 * val interceptFeature = Iterable(("intercept", 3.0))  // AND
 * val incomeFeature = Iterable(("income", [the income value converted to a double]))
 * }}}
 *
 * For more information, see [[com.eharmony.aloha.models.reg.RegressionModelValueToTupleConversions]].
 *
 * @param modelId An identifier for the model.  User in score and error reporting.
 * @param featureNames feature names (parallel to featureFunctions)
 * @param featureFunctions feature extracting functions.
 * @param beta representation of the regression model parameters.
 * @param invLinkFunction a function applied to the inner product of the input vector and weight vector.
 * @param spline an optional calibration spline to
 *               [[http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.29.3039 Obtaining calibrated probability
 *                 estimates from decision trees and naive Bayesian classifiers]], Zadrozny, Elkan (ICML, 2001).  This
 *                 is applied prior to invLinkFunction
 * @param numMissingThreshold if provided, we check whether the threshold is exceeded.  If so, return an error instead
 *                            of the computed score.  This is for missing data situations.
 * @tparam A model input type
 * @tparam B model output type.  Requires a implicit [[com.eharmony.aloha.score.conversions.ScoreConverter]]
 *           to convert from B to com.eharmony.aloha.score.Scores.Score
 */
case class RegressionModel[U, -A, +B <: U](
  modelId: ModelIdentity,
  featureNames: sci.IndexedSeq[String],
  featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
  beta: PolynomialEvaluationAlgo,
  invLinkFunction: Double => Double,
  spline: Option[Spline],
  numMissingThreshold: Option[Int],
  auditor: Auditor[U, Double, B])
extends SubmodelBase[U, Double, A, B]
   with RegressionFeatures[A]
   with Logging {

  debug({
    val rawFeatureDescriptors = (for {
      ff <- featureFunctions
      a <- ff.accessors
    } yield a.descriptor).sorted
    "raw feature names: " + rawFeatureDescriptors.mkString(",")
  })

  /**
    * Get the score.
    *
    * @param a     the model input value.
    * @return
    */
  override def subvalue(a: A): Subvalue[B, Double] = {
    val Features(x, missing, missingOk) = constructFeatures(a)

    debug("x\n\t" + featureNames.zip(x).map { case (name, f) => s"$name -> $f" }.mkString("\n\t"))

    // Before determining the inner product, we know whether we actually should compute it or whether a
    // data error has occurred the will prevent the computation.
    val out =
      if (missingOk) {
        val eta = beta at x
        debug(s"eta: $eta")
        val splinedEta = spline.map(_(eta)) getOrElse eta
        debug(s"splined eta: $splinedEta")
        val mu = invLinkFunction(splinedEta) // Currently, really just a casting operation.
        debug(s"mu: $mu")

        success(mu, missingVarNames = missing.values.flatten.toSet)
      } else {
        failure(Seq("Missing too much data in features: " + missing.keys.toIndexedSeq.sorted), missing.values.flatten.toSet)
      }

    out
  }
}

object RegressionModel extends ParserProviderCompanion with RegressionModelJson {

  import spray.json._

  object Parser extends ModelSubmodelParsingPlugin
                   with EitherHelpers
                   with RegFeatureCompiler {

    val modelType = "Regression"

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[RegressionModel[U, A, B]]] = {

      if (r != RefInfo[Double] && r != RefInfo[jl.Double])
        None
      else {
        Some(new JsonReader[RegressionModel[U, A, B]] {
          override def read(json: JsValue): RegressionModel[U, A, B] = {

            val aud = auditor.asInstanceOf[Auditor[U, Double, B]]

            // Get the metadata necessary to create the model.
            val d = json.convertTo[RegData]

            // Turn the map of features into a Seq to fix the order for all subsequent operations because they
            // need a common understanding of the indices for the features.
            val featureMap: Seq[(String, Spec)] = d.features.toSeq
            val featureNameToIndex: Map[String, Int] = featureMap.map(_._1).zipWithIndex.toMap

            // This is the weight vector.
            val beta = getBeta(d.features.size, d.weights, higherOrderFeatures(d, featureNameToIndex))

            val (featureNames, featureFns) = features(featureMap, semantics).fold(f => throw new DeserializationException(f.mkString("\n")), identity).toIndexedSeq.unzip

            val m = RegressionModel(d.modelId, featureNames, featureFns, beta, identity, d.spline, d.numMissingThreshold, aud)
            m
          }
        })
      }
    }

    /**
      * Translate the specification of higher order features to something a
      * [[com.eharmony.aloha.models.reg.PolynomialEvaluator]].builder can understand.
      * @param d regression model metadata
      * @param featureNameToIndex mapping from feature name to index in the vector of features.
      * @return
      */
    private[this] def higherOrderFeatures(d: RegData, featureNameToIndex: Map[String, Int]): Seq[(Seq[(String, Int)], Double)] = {
      val hof = d.higherOrderFeatures.getOrElse(Nil).map { h =>
        val f = h.features.toSeq.flatMap {
          case (k, v) =>
            val kI = featureNameToIndex(k)
            v.zip(Stream continually kI)
        }
        (f, h.wt)
      }

      hof
    }

    /**
      * Construct a polynomial evaluator given the first order weights (in weights field) and the higher order
      * features.
      * @param n number of features in the feature (generation) vector.
      * @param foWeights the weight map for first order weights.
      * @param higherOrderFeatures the higher order features (order > 1)
      * @return
      */
    private[this] def getBeta(n: Int, foWeights: Map[String, Double], higherOrderFeatures: TraversableOnce[(TraversableOnce[(String, Int)], Double)]) = {
      val p = PolynomialEvaluator.builder.
        addAllFirstOrder(foWeights, n).
        ++=(higherOrderFeatures).
        result()
      p
    }
  }

  def parser: ModelParser = Parser
}
