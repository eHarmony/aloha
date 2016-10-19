package com.eharmony.aloha.models.vw.jni

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic._
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.func.GenAggFunc
import vowpalWabbit.learner.VWLearner

import scala.collection.{mutable => scm, immutable => sci}
import scala.util.{Failure, Success, Try}

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
                                                     learnerCreator: VWLearner => Either[String => B, (String, Long) => B], // Not completely sure if Either is the best way to represent a Union in Scala
                                                     numMissingThreshold: Option[Int] = None,
                                                     salt: Option[GenAggFunc[A, Long]]
                                                   )(implicit private[this] val scb: ScoreConverter[B])
  extends VwJniModel[A, B] {

  /**
    * This is the function responsible to scoring, using VW.
    */
  @transient private[this] lazy val learnerEvaluator= learnerCreator(vwLearner)

  {
    require(
      featureNames.size == featureFunctions.size,
      s"featureNames.size (${featureNames.size}}) != featureFunctions.size (${featureFunctions.size}})")

    val req = sci.BitSet(featureFunctions.indices:_*)
    val act = (sci.BitSet(defaultNs:_*) /: namespaces)(_ ++ _._2)
    require(
      req == act,
      s"defaultNamespace and namespaces must cover all indices (0 until ${featureFunctions.size}).  Missing indices: ${(req -- act).map(i => s"$i='${featureNames(i)}'").mkString(", ")}.")

    // Initialize the lazy vals.  This is done so that errors will be thrown on creation.
    require(vwRowCreator != null)
    require(vwLearner != null)
    require(learnerEvaluator != null)
  }

  /** Produce a score.
    * @param a an input to the model representing covariate data.
    * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
    * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
    *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
    *         sub-model scores.
    */
  override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
    generateVwInput(a) match {
      case Left(missing) => failure(Seq(s"Too many features with missing variables: ${missing.count(_._2.nonEmpty)}"), getMissingVariables(missing))
      case Right(vwIn) =>
        Try { learnerEvaluator.fold(_(vwIn), _(vwIn, salt.get.apply(a))) } match {
          case Success(y) => success(y)
          case Failure(ex) => failure(Seq(ex.getMessage))
        }
    }
  }

  /**
    * Close the underlying VW model.
    */
  override def close(): Unit = vwLearner.close()


  // TODO: Figure out how to make the missing features fast.
  private[jni] def getMissingVariables(missing: scm.Map[String, Seq[String]]): Seq[String] =
  missing.unzip._2.foldLeft(Set.empty[String])(_ ++ _).toIndexedSeq.sorted

  /**
    * Get a VW line (on the right) or a map of missing features (on the left) if there was too much
    * missing data to form a prediction.
    * @param a input
    * @return a result
    */
  private[jni] def generateVwInput(a: A): Either[scm.Map[String, Seq[String]], String] = {
    val Features(features, missing, missingOk) = constructFeatures(a)
    if (missingOk) Right(vwRowCreator.unlabeledVwInput(features).toString)
    else           Left(missing)
  }

}
