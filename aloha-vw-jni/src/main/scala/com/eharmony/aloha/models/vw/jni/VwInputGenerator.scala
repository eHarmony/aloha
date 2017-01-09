package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha.dataset.SparseFeatureExtractorFunction
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.models.reg.RegressionFeatures
import com.eharmony.aloha.semantics.func.GenAggFunc

import scala.collection.{immutable => sci, mutable => scm}

/**
  * Created by sahil-goyal on 10/28/16.
  */
sealed trait VwInputGenerator[-A, +I]
  extends (A => Either[scm.Map[String, Seq[String]], I]) {

  private[jni] val defaultNs: List[Int]
  private[jni] val namespaces: List[(String, List[Int])]

  /**
    * The object responsible to taking the computed features and turning it into a VW input formatted string.
    * This is a transient lazy val so that we can properly serialize
    */
  private[jni] val vwRowCreator: VwRowCreator[A]

  {
    require(vwRowCreator != null)
  }

}

final case class VwSingleLineInputGenerator[-A](
                                                 featureNames: sci.IndexedSeq[String],
                                                 featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
                                                 defaultNs: List[Int],
                                                 namespaces: List[(String, List[Int])],
                                                 numMissingThreshold: Option[Int]
                                               )
  extends VwInputGenerator[A, String]
    with RegressionFeatures[A] {

  @transient private[jni] lazy val vwRowCreator =
    new VwRowCreator(SparseFeatureExtractorFunction(featureNames zip featureFunctions), defaultNs, namespaces, None)

  def apply(v1: A): Either[scm.Map[String, Seq[String]], String] = {
    val Features(features, missing, missingOk) = constructFeatures(v1)
    if (missingOk) Right(vwRowCreator.unlabeledVwInput(features).toString)
    else           Left(missing)
  }

}

final case class VwMultiLineInputGenerator[-A](
                                                featureNames: sci.IndexedSeq[String],
                                                featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
                                                defaultNs: List[Int],
                                                namespaces: List[(String, List[Int])],
                                                numMissingThreshold: Option[Int],
                                                labelDependentFeatureDefaultNs: List[Int],
                                                labelDependentFeatureNamespaces: List[(String, List[Int])],
                                                labelDomainFn: GenAggFunc[A, Seq[Any]],
                                                labelDependentFeatureNames: sci.IndexedSeq[String],
                                                labelDependentFeatureFunctions: sci.IndexedSeq[GenAggFunc[Any, Iterable[(String, Double)]]],
                                                numMissingLDFThreshold: Option[Int]
                                              )
  extends VwInputGenerator[A, Array[String]]
    with VwMultiLineFeatures[A] {

  @transient private[jni] lazy val vwRowCreator =
    new VwRowCreator(SparseFeatureExtractorFunction(featureNames zip featureFunctions), defaultNs, namespaces, None)

  @transient private[jni] lazy val labelDependentRowCreator = new VwRowCreator(
    SparseFeatureExtractorFunction(labelDependentFeatureNames zip labelDependentFeatureFunctions),
    labelDependentFeatureDefaultNs,
    labelDependentFeatureNamespaces,
    None
  )

  {
    require(labelDependentRowCreator != null)
  }

  def apply(v1: A): Either[scm.Map[String, Seq[String]], Array[String]] = {
    val Features(features, missing, missingOk) = constructMultiLineFeatures(v1)
    if (missingOk) {
      val sharedFeatures = s"shared ${vwRowCreator.unlabeledVwInput(features._1).toString}"
      val labelDependentFeatures = features._2.map(f => labelDependentRowCreator.unlabeledVwInput(f).toString)
      if(features._1.exists(f => f.nonEmpty)) Right(sharedFeatures +: labelDependentFeatures)
      else Right(labelDependentFeatures)
    }
    else Left(missing)
  }

}
