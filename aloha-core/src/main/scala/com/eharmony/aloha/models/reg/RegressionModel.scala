package com.eharmony.aloha.models.reg

import java.{lang => jl}

import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.factory.pimpz.JsValuePimpz
import com.eharmony.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.reg.json.{RegressionModelJson, Spec}
import com.eharmony.aloha.models.{BaseModel, TypeCoercion}
import com.eharmony.aloha.reflect.RefInfoOps
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, Logging}

import scala.collection.{immutable => sci}
import scala.language.{higherKinds, implicitConversions}

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
case class RegressionModel[-A, +B: ScoreConverter](
  modelId: ModelIdentity,
  featureNames: sci.IndexedSeq[String],
  featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
  beta: PolynomialEvaluationAlgo,
  invLinkFunction: Double => B,
  spline: Option[Spline],
  numMissingThreshold: Option[Int])
extends BaseModel[A, B]
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
   * @param a the model input value.
   * @param audit whether to audit the output.
   * @return
   */
  private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
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

        success(mu, missing.values.flatten)
      } else {
        failure(Seq("Missing too much data in features: " + missing.keys.toIndexedSeq.sorted), missing.values.flatten)
      }

    out
  }
}

object RegressionModel extends ParserProviderCompanion with JsValuePimpz with RegressionModelJson {
  import spray.json._

  object Parser extends ModelParserWithSemantics with EitherHelpers {
    val modelType = "Regression"

    /**
     *
     * @param semantics This reader requires semantics to be provided (some).  Otherwise, an error will occur. This
     *                  is because the regression models create functions for each feature in the model and
     *                  function creation is performed by the semantics.
     * @tparam A input type of the model
     * @tparam B output type of the model
     * @return
     */
    def modelJsonReader[A, B: JsonReader: ScoreConverter](semantics: Semantics[A]): JsonReader[RegressionModel[A, B]] = new JsonReader[RegressionModel[A, B]] {
      def read(json: JsValue): RegressionModel[A, B] = {

        // Get the metadata necessary to create the model.
        val d = json.convertTo[RegData]

        // Turn the map of features into a Seq to fix the order for all subsequent operations because they
        // need a common understanding of the indices for the features.
        val featureMap: Seq[(String, Spec)] = d.features.toSeq
        val featureNameToIndex: Map[String, Int] = featureMap.map(_._1).zipWithIndex.toMap

        // This is the weight vector.
        val beta = getBeta(d.features.size, d.weights, higherOrderFeatures(d, featureNameToIndex))

        // Get the function that coerces to the output type.
        implicit val rib = implicitly[ScoreConverter[B]].ri

        val cf = TypeCoercion[Double, B] getOrElse {
          throw new DeserializationException("Couldn't find conversion function for RegressionModel with output type: " + RefInfoOps.toString(rib))
        }

        val (featureNames, featureFns) = features(featureMap, semantics).fold(f => throw new DeserializationException(f.mkString("\n")), identity).toIndexedSeq.unzip
        val m = RegressionModel[A, B](d.modelId, featureNames, featureFns, beta, cf, d.spline, d.numMissingThreshold)
        m
      }
    }

    /**
     * Translate the feature specification into features.  This is done in a short circuiting way so that it
     * stops when the any feature cannot be produced.
     *
     * @param featureMap a map of feature name to feature specification
     * @param semantics a semantics with which feature specifications should be interpretted.
     * @tparam A model input type
     * @return a mapping from feature name to feature function.  Note that the indices matter and that's why we
     *         don't want to use a map.
     */
    private[this] def features[A](featureMap: Seq[(String, Spec)], semantics: Semantics[A]) =
      mapSeq(featureMap) {
        case (k, Spec(spec, default)) =>
          semantics.createFunction[Iterable[(String, Double)]](spec, default).
            left.map { Seq(s"Error processing spec '$spec'") ++ _ }. // Add the spec that errored.
            right.map { f => (k, f) }
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
        {
          val f = h.features.toSeq.flatMap {
            case (k, v) =>
              val kI = featureNameToIndex(k)
              v.zip(Stream continually kI)
          }
          (f, h.wt)
        }
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
