package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.func.GenAggFunc
import vowpalWabbit.learner.VWLearner

import scala.collection.{immutable => sci}

/**
  * Created by sahil-goyal on 9/6/16.
  */
final case class VwSingleFeatureSetJniModel[-A, +B](
                                                     modelId: ModelIdentity,
                                                     vwParams: String,
                                                     modelSource: ModelSource,
                                                     featureNames: sci.IndexedSeq[String],
                                                     featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
                                                     defaultNs: List[Int],
                                                     namespaces: List[(String, List[Int])],
                                                     learnerCreator: VWLearner => VwEvaluator[A, String, (B, Option[Float])],
                                                     numMissingThreshold: Option[Int] = None
                                                   )(implicit private[this] val scb: ScoreConverter[B])
  extends VwJniModel[A, B] {

  type VwInput = String

  val vwInputGen: VwSingleLineInputGenerator[A] = VwSingleLineInputGenerator(
    featureNames,
    featureFunctions,
    defaultNs,
    namespaces,
    numMissingThreshold
  )

}
