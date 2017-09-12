package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.{Closeable, File}

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.multilabel.SparseMultiLabelPredictor
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}
import vowpalWabbit.responses.ActionScores

import scala.collection.{immutable => sci}
import scala.util.Try

/**
  * Created by ryan.deak on 9/8/17.
  * @param modelSource a specification of a location for the underlying VW model that
  *                    will be materialized in this class.
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
    params: String,
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])])
extends SparseMultiLabelPredictor[K]
   with Closeable {

  import VwSparseMultilabelPredictor._

  @transient private[multilabel] lazy val vwModel = createLearner(modelSource, params).get

  {
    val emptyNss = namespaces collect { case (ns, ind) if ind.isEmpty => ns }
    require(
      emptyNss.isEmpty,
      s"There should be no namespaces that are empty.  Found: ${emptyNss mkString ", "}"
    )

    // Force creation.
    require(vwModel != null)
  }

  override def apply(
      features: IndexedSeq[Sparse],
      labels: sci.IndexedSeq[K],
      indices: sci.IndexedSeq[Int],
      labelDependentFeatures: sci.IndexedSeq[IndexedSeq[Sparse]]
  ): Try[Map[K, Double]] = {
    val x = multiLabelClassifierInput(features, indices, defaultNs, namespaces)
    val pred = Try { vwModel.predict(x) }
    val yOut = pred.map { y => produceOutput(y, labels) }
    yOut
  }

  override def close(): Unit = vwModel.close()
}

object VwSparseMultilabelPredictor {
  private val ClassNS = "Y"

  private[multilabel] def multiLabelClassifierInput(
      features: IndexedSeq[Sparse],
      indices: sci.IndexedSeq[Int],
      defaultNs: List[Int],
      namespaces: List[(String, List[Int])]
  ): Array[String] = {
    val n = indices.size
    // The length of the output array is n + 1.  The first row is the shared features.
    // These are features that are not label dependent.  Then come the features for the
    // n labels.
    val x = new Array[String](n + 1)

    val shared = VwRowCreator.unlabeledVwInput(features, defaultNs, namespaces, false)
    // "shared" is a special keyword in VW multi-class (multi-row) format.
    // See:  https://www.umiacs.umd.edu/%7Ehal/tmp/multiclassVW.html
    x(0) = "shared " + shared

    // This is mutable because we want speed.
    var i = 0

    while (i < n) {
      val labelInd = indices(i)
      x(i + 1) = s"$labelInd:0 |$ClassNS _C${labelInd}_"
      i += 1
    }

    x
  }

  private[multilabel] def produceOutput[K](pred: ActionScores, labels: sci.IndexedSeq[K]): Map[K, Double] = {

    // The last two action IDs in the action scores array are the dummy actions.
    (for {
      as    <- pred.getActionScores // if as.getAction < indices.size if we need to deal with dummy classes.
      label  = labels(as.getAction)
      pred   = modifiedLogistic(as.getScore)
    } yield label -> pred)(collection.breakOut)
  }

  /**
    * A modified logistic function where the sign of the exponent is opposite the usual
    * definition.  Since CSOAA in VW employs costs, it changes the sign of the normal
    * logistic function so the definition becomes `1 / (1 + exp(x))`.
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
    * @param modelSource
    * @param params
    * @return
    */
  // TODO: How much of the parameter setup is up to the caller versus this function?
  private[multilabel] def paramsWithSource(modelSource: File, params: String): String =
    params + " -i" + modelSource.getCanonicalPath + " -t --quiet"

  private[multilabel] def createLearner(modelSource: ModelSource, params: String): Try[VWActionScoresLearner] = {
    val modelFile = modelSource.localVfs.replicatedToLocal()
    val updatedParams = paramsWithSource(modelFile.fileObj, params)
    Try { VWLearners.create[VWActionScoresLearner](updatedParams) }
  }
}
