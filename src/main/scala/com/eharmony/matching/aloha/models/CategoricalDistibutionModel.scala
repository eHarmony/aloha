package com.eharmony.matching.aloha.models

import scala.collection.immutable
import spray.json.{ JsonFormat, JsValue, JsonReader }
import spray.json.DefaultJsonProtocol._
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.rand.HashedCategoricalDistribution
import com.eharmony.matching.aloha.factory.{ ModelParserWithSemantics, ModelParser, ParserProviderCompanion }
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.util.Logging

/**
 * A model representing a categorical distribution.  This will return values with the probabilities prescribed by the
 * distribution parameter.  For information on
 * [[http://en.wikipedia.org/wiki/Categorical_distribution categorical distribution]]s, check out Wikipedia's page.
 * @param modelId An id with which to identify this model
 * @param features features whose values are fed to the distribution.  These features are functions of the input.
 * @param distribution A distribution parametrized by a sequence of probabilities, that takes a sequence of values and
 *                     produces a hash that is used as the randomness with which to choose one of the labels.
 * @param labels the values that can returned by this model (with the probabilities described by the distribution)
 * @param missingOk Whether to allow missing data defaults to false).  When this is set to false and missing data (
 *                  scala.None) is produced by one of the features, the model will result in an error.
 * @tparam A model input type
 * @tparam B model output type
 */
case class CategoricalDistibutionModel[-A, +B: ScoreConverter](
  modelId: ModelIdentity,
  features: Seq[GenAggFunc[A, Any]],
  distribution: HashedCategoricalDistribution,
  labels: immutable.IndexedSeq[B],
  missingOk: Boolean = false) extends BaseModel[A, B] {

  import CategoricalDistibutionModel.missingMsg

  require(1 <= features.size, "There should be at least one feature on which the hash is based.  Received 0 features.")
  require(distribution.numClasses == labels.size, s"number of classes in distribution (${distribution.numClasses}})should equal the number of labels (${labels.size}}).")

  /**
   * "Randomly" but ''idempotently'' pick a label based on the probabilities in the distribution.
   * @param a input from which features are extracted.  These features are then hashed to produce a value.
   * @return a positive value i if node i should be selected.  May return a negative value in which case processErrorAt
   *         should be called with the value returned.
   */
  private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
    var featuresWithMissing: List[GenAggFunc[A, Any]] = Nil

    // Compute the features.  Note that we don't use flatMap because:
    //   [Some(x_1), ..., Some(x_i-1), None, Some(v), Some(x_i+2), ..., Some(x_n)]    AND
    //   [Some(x_1), ..., Some(x_i-1), Some(v), None, Some(x_i+2), ..., Some(x_n)]
    // produce different hashes but if we applied flatMap, they would give the same hash value.
    val x = features.map { f =>
      {
        val y = f(a)
        if (None == y) featuresWithMissing = f :: featuresWithMissing
        y
      }
    }

    if (featuresWithMissing.isEmpty) success(labels(distribution(x)))
    else if (missingOk) success(labels(distribution(x)), missing(featuresWithMissing, a))
    else failure(missingMsg, missing(featuresWithMissing, a))
  }

  /**
   * Get the missing features.
   * @param featuresWithMissing a non-empty list of features that had missing values
   * @param a model input
   * @return
   */
  private[this] def missing(featuresWithMissing: List[GenAggFunc[A, Any]], a: A) =
    featuresWithMissing.flatMap { _ accessorOutputMissing a }.distinct.sorted

  override def toString = s"CategoricalDistibutionModel($modelId, $features, $distribution, $missingOk)"
}

object CategoricalDistibutionModel extends ParserProviderCompanion {
  private val missingMsg = Seq("Couldn't choose random output due to missing features")

  object Parser extends ModelParserWithSemantics with Logging {
    val modelType = "CategoricalDistribution"

    private case class Ast[B: JsonFormat: ScoreConverter](
      features: Seq[String],
      probabilities: Seq[Double],
      labels: immutable.IndexedSeq[B],
      missingOk: Option[Boolean] = None)

    private implicit def astFormatReader[B: JsonFormat: ScoreConverter] = jsonFormat4(Ast.apply[B])

    def modelJsonReader[A, B: JsonReader: ScoreConverter](semantics: Semantics[A]) = new JsonReader[CategoricalDistibutionModel[A, B]] {
      def read(json: JsValue) = {
        implicit val bFormat = jsonReaderToJsonFormat[B]
        val id = getModelId(json).get
        val ast = json.convertTo[Ast[B]]
        val featuresX = ast.features.map(f => semantics.createFunction[Any](f))
        val features = featuresX.map(_.left.map(e => error("errors: " + e.mkString("\n") + "json: " + json.compactPrint)).right.get)

        val m = CategoricalDistibutionModel[A, B](
          id,
          features,
          HashedCategoricalDistribution(ast.probabilities: _*),
          ast.labels,
          ast.missingOk.getOrElse(false))

        m
      }
    }
  }

  def parser: ModelParser = Parser
}
