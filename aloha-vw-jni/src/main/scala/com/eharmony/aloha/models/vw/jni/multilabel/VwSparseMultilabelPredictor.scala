package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.Closeable

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.multilabel.SparseMultiLabelPredictor
import vowpalWabbit.responses.ActionScores
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}

import scala.collection.{immutable => sci}
import scala.util.Try

/**
  * Created by ryan.deak on 9/8/17.
  */
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
    // Force creation.
    require(vwModel != null)
  }

  override def apply(
      features: IndexedSeq[Sparse],
      labels: sci.IndexedSeq[K],
      indices: sci.IndexedSeq[Int],
      labelDependentFeatures: sci.IndexedSeq[IndexedSeq[Sparse]]
  ): Map[K, Double] = {
    val x = constructInput(features, indices, defaultNs, namespaces)
    val pred = Try { vwModel.predict(x) }
    val yOut = pred.map { y => produceOutput(y, labels, indices) }

    // TODO: Change the interface to Try[Map[K, Double]]
    yOut.get
  }

  override def close(): Unit = vwModel.close()
}

object VwSparseMultilabelPredictor {

  private[multilabel] def constructInput[K](
      features: IndexedSeq[Sparse],
      indices: sci.IndexedSeq[Int],
      defaultNs: List[Int],
      namespaces: List[(String, List[Int])]
  ): Array[String] = {
    ???
  }

  private[multilabel] def produceOutput[K](
      pred: ActionScores,
      labels: sci.IndexedSeq[K],
      indices: sci.IndexedSeq[Int]
  ): Map[K, Double] = {
    val n = labels.size

    // TODO: Possibly update the interface to pass this in (possibly non-strictly).
    val indToLabel: Map[Int, K] = indices.zip(labels)(collection.breakOut)

    val y: Map[K, Double] = (for {
      as <- pred.getActionScores
      label <- indToLabel.get(as.getAction).toIterable
      pred = as.getScore.toDouble
    } yield label -> pred)(collection.breakOut)

    y
  }

  private[multilabel] def paramsWithSource(modelSource: ModelSource, params: String): String = {
    // TODO: Fill in.
    ???
  }

  private[multilabel] def createLearner(modelSource: ModelSource, params: String): Try[VWActionScoresLearner] = {
    val modelFile = modelSource.localVfs.replicatedToLocal()
    val updatedparams = paramsWithSource(modelSource, params)

    // : _root_.com.eharmony.aloha.io.vfs.File
    val vfs = modelSource.localVfs
    Try { VWLearners.create[VWActionScoresLearner](params) }
  }
}
