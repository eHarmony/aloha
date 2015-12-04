package com.eharmony.aloha.models.h2o.categories

import com.eharmony.aloha.models.TypeCoercion
import com.eharmony.aloha.models.h2o.{IllConditionedMultinomial, IllConditionedScalar, IllConditioned}
import com.eharmony.aloha.reflect.RefInfo
import hex.ModelCategory
import hex.ModelCategory._
import hex.genmodel.easy.{EasyPredictModelWrapper, RowData}
import scala.Double.{NaN, NegativeInfinity, PositiveInfinity}
import java.{lang => jl}

sealed trait PredictionFuncRetrievalError

// s"H2O model type ${d.name} is not supported"
case class UnsupportedModelCategory(modelCategory: ModelCategory) extends PredictionFuncRetrievalError

// s"For ${model.getModelCategory.name} model: no type coercion available from ${RefInfoOps.toString[C]} to ${RefInfoOps.toString[B]}"
case class TypeCoercionNotFound(modelCategory: ModelCategory) extends PredictionFuncRetrievalError


sealed trait H2oModelCategory[C] {
  protected[this] implicit def ri: RefInfo[C]
  protected[this] def predictionFn(model: EasyPredictModelWrapper): RowData => Either[IllConditioned, C]

  final def predictor[B: RefInfo](model: EasyPredictModelWrapper): Either[TypeCoercionNotFound, RowData => Either[IllConditioned, B]] =
    TypeCoercion[C, B] map { coercion =>
      predictionFn(model) andThen (_.right map coercion)
    } toRight noCoercionFound(model)

  private[this] def noCoercionFound(model: EasyPredictModelWrapper) =
    TypeCoercionNotFound(model.getModelCategory)
}

object H2oModelCategory {
  private[categories]def wellConditioned(d: Double) = jl.Double.compare(NaN, d) != 0 &&
                                                      PositiveInfinity != d          &&
                                                      NegativeInfinity != d

  private[categories] def filterWellConditionedScalar(d: Double) =
    if (wellConditioned(d)) Right(d) else Left(IllConditionedScalar(d))

  /**
   * Construct a function from H,,2,,O's EasyPredictModelWrapper's input type (''RowData'') to the
   * output type for this Aloha model instance.  If the function could be produced, it will be on
   * the right.  Otherwise, An error will be on the left.
   *
   * Note that different classes of H,,2,,O models have different return types.
   *
   1. '''Binomial''' returns the probability of the positive event.
   1. '''Clustering''' returns the index of the assigned cluster.
   1. '''Multinomial''' returns the index of the selected class.
   1. '''Regression''' returns the regression value.
   *
   * @param model a model to use for prediction.
   * @return
   */
  def predictor[B: RefInfo](model: EasyPredictModelWrapper): Either[PredictionFuncRetrievalError, RowData => Either[IllConditioned, B]] = {
    model.getModelCategory match {
      case Binomial    => BinomialModel.predictor(model)
      case Clustering  => ClusteringModel.predictor(model)
      case Multinomial => MultinomialModel.predictor(model)
      case Regression  => RegressionModel.predictor(model)
      case category    => Left(UnsupportedModelCategory(category))
    }
  }
}

case object ClusteringModel extends H2oModelCategory[Int] {
  protected[this] implicit def ri = RefInfo[Int]
  protected[this] def predictionFn(model: EasyPredictModelWrapper) =
    d => Right(model.predictClustering(d).cluster)
}

case object BinomialModel extends H2oModelCategory[Double] {
  protected[this] implicit def ri = RefInfo[Double]
  protected[this] def predictionFn(model: EasyPredictModelWrapper) =
    d => H2oModelCategory.filterWellConditionedScalar(model.predictBinomial(d).classProbabilities(1))
}

case object MultinomialModel extends H2oModelCategory[Int] {
  protected[this] implicit def ri = RefInfo[Int]
  protected[this] def predictionFn(model: EasyPredictModelWrapper) =
    d => {
      val p = model.predictMultinomial(d)
      val pr = p.classProbabilities
      val i = p.labelIndex
      if (H2oModelCategory.wellConditioned(pr(i))) Right(i)
      else Left(IllConditionedMultinomial(pr.toVector, i))
    }
}

case object RegressionModel extends H2oModelCategory[Double] {
  protected[this] val ri = RefInfo[Double]
  protected[this] def predictionFn(model: EasyPredictModelWrapper) =
    d => H2oModelCategory.filterWellConditionedScalar(model.predictRegression(d).value)
}
