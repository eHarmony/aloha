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
  *
  * Created by ryan.deak on 9/8/17.
  * @param modelSource a specification for the underlying ''Cost Sensitive One Against All''
  *                    VW model with ''label dependent features''.  VW flag
  *                    `--csoaa_ldf mc` is expected. For more information, see the
  *                    [[https://github.com/JohnLangford/vowpal_wabbit/wiki/Cost-Sensitive-One-Against-All-(csoaa)-multi-class-example VW CSOAA wiki page]].
  *                    Also see the ''Cost-Sensitive Multiclass Classification'' section of
  *                    Hal Daume's [[https://www.umiacs.umd.edu/%7Ehal/tmp/multiclassVW.html On Multiclass Classification in VW]]
  *                    page.  This model specification will be materialized in this class.
  * @param params VW parameters.
  * @param defaultNs The list of indices into the `features` sequence that does not have
  *                  an exist in any value of the `namespaces` map.
  * @param namespaces Mapping from namespace name to indices in the `features` sequence passed
  *                   to the `apply` method.  There should be no empty namespaces, meaning
  *                   ''key-value'' pairs appearing in the map should not values that are empty
  *                   sequences.  '''This is a requirement.'''
  * @tparam K the label or class type.
  */
// TODO: Comment this function.  It requires a lot of assumptions.  Make those known.
case class VwSparseMultilabelPredictor[K](
    modelSource: ModelSource,

    // TODO: Should these be removed?  I don't think so but could be, w/o harm, in limited cases.
    params: String,
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    numLabelsInTrainingSet: Int)
extends SparseMultiLabelPredictor[K]
   with Closeable {

  import VwSparseMultilabelPredictor._

  @transient private[this] lazy val paramsAndVwModel =
    createLearner(modelSource, params, numLabelsInTrainingSet)

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

    // TODO: Pass ClassNS in via the constructor
    val x = VwMultilabelRowCreator.predictionInput(features, indices, defaultNs, namespaces, ClassNS)
    val pred = Try { vwModel.predict(x) }
    val yOut = pred.map { y => produceOutput(y, labels) }
    yOut
  }

  override def close(): Unit = vwModel.close()
}

object VwSparseMultilabelPredictor {
  private val ClassNS = "Y"

  private[this] val AddlVwRingSize = 10

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
    * Update the parameters with the
    *
    * VW params of interest when doing multi-class:
    *
    - `--csoaa_ldf mc`            Label-dependent features for multi-class classification
    - `--csoaa_rank`              (Probably) necessary to get scores for m-c classification.
    - `--loss_function logistic`  Standard logistic loss for learning.
    - `--noconstant`              Don't want a constant since it's not interacted with NS Y.
    - `-q YX`                     Cross product of label-dependent and features and features
    - `--ignore_linear Y`         Don't care about the 1st-order wts of the label-dep features.
    - `--ignore_linear X`         Don't care about the 1st-order wts of the features.
    - `--ignore y`                Ignore everything related to the dummy class instances.
    *
    * {{{
    * val str =
    *   "shared |X feature"  + "\n" +
    *
    *   "0:1 |y _C0_"        + "\n" +  // These two instances are dummy classes
    *   "1:0 |y _C1_"        + "\n" +
    *
    *   "2:0 |Y _C2_"        + "\n" +
    *   "3:1 |Y _C3_"
    *
    * val ex = str.split("\n")
    * }}}
    * @param modelSource location of a VW model.
    * @param params the parameters passed to the model to which additional parameters will be added.
    * @return
    */
  // TODO: How much of the parameter setup is up to the caller versus this function?
  private[multilabel] def paramsWithSource(
      modelSource: File,
      params: String,
      numLabelsInTrainingSet: Int
  ): String = {
    val ringSize = numLabelsInTrainingSet + AddlVwRingSize
    s"$params -i ${modelSource.getCanonicalPath} --ring_size $ringSize --testonly --quiet"
  }

  private[multilabel] def createLearner(
      modelSource: ModelSource,
      params: String,
      numLabelsInTrainingSet: Int
  ): (String, Try[VWActionScoresLearner]) = {
    val modelFile = modelSource.localVfs.replicatedToLocal()
    val updatedParams = paramsWithSource(modelFile.fileObj, params, numLabelsInTrainingSet)
    val learner = Try { VWLearners.create[VWActionScoresLearner](updatedParams) }
    (updatedParams, learner)
  }
}
