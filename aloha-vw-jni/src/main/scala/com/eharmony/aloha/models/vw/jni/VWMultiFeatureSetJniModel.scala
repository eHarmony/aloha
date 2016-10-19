package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic._
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.func.GenAggFunc
import vowpalWabbit.learner.VWLearner

import scala.collection.{immutable => sci}

/**
  * Created by sahil-goyal on 9/6/16.
  */
final case class VWMultiFeatureSetJniModel[-A, +B, LabelType](
                                                               modelId: ModelIdentity,
                                                               vwParams: String,
                                                               modelSource: ModelSource,
                                                               featureNames: sci.IndexedSeq[String],
                                                               featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
                                                               labelDomainFn: GenAggFunc[A, sci.IndexedSeq[LabelType]],
                                                               ldFeatureNames: sci.IndexedSeq[String],
                                                               ldFeatureFunctions: sci.IndexedSeq[GenAggFunc[LabelType, Iterable[(String, Double)]]],
                                                               defaultNs: List[Int],
                                                               namespaces: List[(String, List[Int])],
                                                               learnerCreator: VWLearner => Array[String] => B,
                                                               numMissingThreshold: Option[Int] = None
                                                             )(implicit private[this] val scb: ScoreConverter[B])
  extends VwJniModel[A, B] {

  override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = ???

}
