package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.{Closeable, File}

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.multilabel.SparseMultiLabelPredictor
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}
import vowpalWabbit.responses.ActionScores

import scala.collection.{immutable => sci}
import scala.util.Try

/**
  * Creates a VW multi-label predictor plugin for `MultilabelModel`.
  * @param modelSource a specification for the underlying ''Cost Sensitive One Against All''
  *                    VW model with ''label dependent features''.  VW flag `--csoaa_ldf mc`
  *                    or `--wap_ldf mc` is expected. For more information, see the
  *                    [[https://github.com/JohnLangford/vowpal_wabbit/wiki/Cost-Sensitive-One-Against-All-(csoaa)-multi-class-example VW CSOAA wiki page]].
  *                    Also see the ''Cost-Sensitive Multiclass Classification'' section of
  *                    Hal Daume's [[https://www.umiacs.umd.edu/%7Ehal/tmp/multiclassVW.html On Multiclass Classification in VW]]
  *                    page.  This model specification will be materialized in this class.
  * @param defaultNs The list of indices into the `features` sequence that does not have
  *                  an exist in any value of the `namespaces` map.
  * @param namespaces Mapping from namespace name to indices in the `features` sequence passed
  *                   to the `apply` method.  There should be no empty namespaces, meaning
  *                   ''key-value'' pairs appearing in the map should not values that are empty
  *                   sequences.  '''This is a requirement.'''
  * @tparam K the label or class type.
  * @author deaktator
  * @since 9/8/2017
  */
case class VwSparseMultilabelPredictor[K](
    modelSource: ModelSource,
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    numLabelsInTrainingSet: Int)
extends SparseMultiLabelPredictor[K]
   with Closeable {

  import VwSparseMultilabelPredictor._

  @transient private[this] lazy val paramsAndVwModel =
    createLearner(modelSource, numLabelsInTrainingSet)

  @transient private[this] lazy val updatedParams = paramsAndVwModel._1
  @transient private[multilabel] lazy val vwModel = paramsAndVwModel._2.get


  {
    val emptyNss = namespaces collect { case (ns, ind) if ind.isEmpty => ns }
    require(
      emptyNss.isEmpty,
      s"There should be no namespaces that are empty.  Found: ${emptyNss mkString ", "}"
    )

    // Force creation.
    require(vwModel != null)
  }

  /**
    * Get the VW parameters used to invoke the underlying VW model.
    * @return VW parameters.
    */
  def vwParams(): String = updatedParams

  /**
    * Given the input, form a VW example, and delegate to the underlying ''CSOAA LDF'' VW model.
    * @param features  (non-label dependent) features shared across all labels.
    * @param labels labels for which the VW learner should produce predictions.
    * @param indices the indices `labels` into the sequence of all labels encountered
    *                during training.
    * @param labelDependentFeatures Any label dependent features.  This is not yet utilized and
    *                               is currently ignored.
    * @return a Map from label to prediction.
    */
  override def apply(
      features: IndexedSeq[Sparse],
      labels: sci.IndexedSeq[K],
      indices: sci.IndexedSeq[Int],
      labelDependentFeatures: sci.IndexedSeq[IndexedSeq[Sparse]]
  ): Try[Map[K, Double]] = {

    val x = VwMultilabelRowCreator.predictionInput(features, indices, defaultNs, namespaces, ClassNS)
    val pred = Try { vwModel.predict(x) }
    val yOut = pred.map { y => produceOutput(y, labels) }
    yOut
  }

  override def close(): Unit = vwModel.close()
}

object VwSparseMultilabelPredictor {
  private val ClassNS = "Y"

  private[multilabel] val AddlVwRingSize = 10

  private[multilabel] type ExpectedLearner = VWActionScoresLearner

  /**
    * Produce the output given VW's output, `pred`, and the labels provided to the `apply` function.
    * @param pred predictions returned by the underlying VW ''CSOAA LDF'' model.
    * @param labels the labels provided to the `apply` function.  This determines which predictions
    *               should be produced.
    * @tparam K The label or class type.
    * @return a map of predictions from label to prediction.
    */
  private[multilabel] def produceOutput[K](pred: ActionScores, labels: sci.IndexedSeq[K]): Map[K, Double] = {
    (for {
      as    <- pred.getActionScores
      label  = labels(as.getAction)
      pred   = modifiedLogistic(as.getScore)
    } yield label -> pred)(collection.breakOut)
  }

  /**
    * A modified logistic function where the sign of the exponent is opposite the usual
    * definition.  Since CSOAA in VW employs costs, it returns the negative logits which
    * changes the sign of the normal logistic function so the definition becomes
    * `1 / (1 + exp(x))`.
    *
    * @param x an input produced by a VW CSOAA prediction.
    * @return a probability.
    */
  @inline final private def modifiedLogistic(x: Float) = 1 / (1 + math.exp(x))

  /**
    * Update parameters with initial regressior, ring size, testonly, and quiet.
    * @param modelSource a trained VW binary model.
    * @param numLabelsInTrainingSet number of labels in the training set informs VW's ring size.
    * @return updated parameters.
    */
  private[multilabel] def paramsWithSource(modelSource: File, numLabelsInTrainingSet: Int): String = {
    val ringSize = numLabelsInTrainingSet + AddlVwRingSize
    s"-i ${modelSource.getCanonicalPath} --ring_size $ringSize --testonly --quiet"
  }

  private[multilabel] def createLearner(
      modelSource: ModelSource,
      numLabelsInTrainingSet: Int
  ): (String, Try[ExpectedLearner]) = {
    val modelFile = modelSource.localVfs.replicatedToLocal()
    val updatedParams = paramsWithSource(modelFile.fileObj, numLabelsInTrainingSet)
    val learner = Try { VWLearners.create[ExpectedLearner](updatedParams) }
    (updatedParams, learner)
  }
}
