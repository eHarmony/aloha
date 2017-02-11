package com.eharmony.aloha.models

import com.eharmony.aloha.audit.Auditor

import scala.collection.immutable
import spray.json.{JsValue, JsonFormat, JsonReader}
import spray.json.DefaultJsonProtocol._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.rand.HashedCategoricalDistribution
import com.eharmony.aloha.factory._
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.util.Logging

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
case class CategoricalDistibutionModel[U, N, -A, +B <: U](
  modelId: ModelIdentity,
  features: Seq[GenAggFunc[A, Any]],
  distribution: HashedCategoricalDistribution,
  labels: immutable.IndexedSeq[N],
  auditor: Auditor[U, N, B],
  missingOk: Boolean = false) extends SubmodelBase[U, N, A, B] {

  import CategoricalDistibutionModel.missingMsg

  require(1 <= features.size, "There should be at least one feature on which the hash is based.  Received 0 features.")
  require(distribution.numClasses == labels.size, s"number of classes in distribution (${distribution.numClasses}})should equal the number of labels (${labels.size}}).")

  /**
   * "Randomly" but ''idempotently'' pick a label based on the probabilities in the distribution.
   * @param a input from which features are extracted.  These features are then hashed to produce a value.
   * @return a positive value i if node i should be selected.  May return a negative value in which case processErrorAt
   *         should be called with the value returned.
   */
  def subvalue(a: A): Subvalue[B, N] = {
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


    if (missingOk) {
      val n = labels(distribution(x))
      if (featuresWithMissing.isEmpty)
        success(n)
      else
        success(n, missingVarNames = missing(featuresWithMissing, a))
    }
    else
      failure(missingMsg, missing(featuresWithMissing, a))
  }

  /**
   * Get the missing features.
   * @param featuresWithMissing a non-empty list of features that had missing values
   * @param a model input
   * @return
   */
  private[this] def missing(featuresWithMissing: List[GenAggFunc[A, Any]], a: A): Set[String] =
    featuresWithMissing.flatMap { _ accessorOutputMissing a }.toSet

  override def toString = s"CategoricalDistibutionModel($modelId, $features, $distribution, $missingOk)"
}

object CategoricalDistibutionModel extends ParserProviderCompanion {
  private val missingMsg = Seq("Couldn't choose random output due to missing features")

  object Parser extends ModelSubmodelParsingPlugin with Logging {
    val modelType = "CategoricalDistribution"

    private case class Ast[N: JsonFormat](
        features: Seq[String],
        probabilities: Seq[Double],
        labels: immutable.IndexedSeq[N],
        missingOk: Option[Boolean] = None)

    private implicit def astFormatReader[B: JsonFormat] = jsonFormat4(Ast.apply[B])

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[CategoricalDistibutionModel[U, N, A, B]]] = {

      Some(new JsonReader[CategoricalDistibutionModel[U, N, A, B]] {
        override def read(json: JsValue): CategoricalDistibutionModel[U, N, A, B] = {
          val id = getModelId(json).get
          val ast = json.convertTo[Ast[N]]
          val featuresX = ast.features.map(f => semantics.createFunction[Any](f))
          val features = featuresX.map(_.left.map(e => error("errors: " + e.mkString("\n") + "json: " + json.compactPrint)).right.get)

          val m = CategoricalDistibutionModel(
            id,
            features,
            HashedCategoricalDistribution(ast.probabilities: _*),
            ast.labels,
            auditor,
            ast.missingOk.getOrElse(false))

          m
        }
      })
    }
  }

  def parser: ModelParser = Parser
}
