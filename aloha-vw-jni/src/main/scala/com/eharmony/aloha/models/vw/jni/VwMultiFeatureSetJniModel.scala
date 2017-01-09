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
final case class VwMultiFeatureSetJniModel[-A, +B](
                                                               modelId: ModelIdentity,
                                                               vwParams: String,
                                                               modelSource: ModelSource,
                                                               featureNames: sci.IndexedSeq[String],
                                                               featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
                                                               defaultNs: List[Int],
                                                               namespaces: List[(String, List[Int])],
                                                               learnerCreator: VWLearner => VwEvaluator[A, Array[String], Either[(Seq[String], Seq[String]), (B, Option[Float])]],
                                                               numMissingThreshold: Option[Int] = None,
                                                               labelDomainFn: GenAggFunc[A, Seq[Any]],
                                                               labelDependentFeatureNames: sci.IndexedSeq[String],
                                                               labelDependentFeatureFunctions: sci.IndexedSeq[GenAggFunc[Any, Iterable[(String, Double)]]],
                                                               labelDependentFeatureDefaultNs: List[Int],
                                                               labelDependentFeatureNamespaces: List[(String, List[Int])],
                                                               numMissingLDFThreshold: Option[Int] = None
                                                             )(implicit private[this] val scb: ScoreConverter[B])
  extends VwJniModel[A, B] {

  override type VwInput = Array[String]

  override val vwInputGen: VwMultiLineInputGenerator[A] = VwMultiLineInputGenerator(
    featureNames,
    featureFunctions,
    defaultNs,
    namespaces,
    numMissingThreshold,
    labelDependentFeatureDefaultNs,
    labelDependentFeatureNamespaces,
    labelDomainFn,
    labelDependentFeatureNames,
    labelDependentFeatureFunctions,
    numMissingLDFThreshold
  )

}
