package com.eharmony.aloha.models.multilabel

import java.io.Closeable

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models._
import com.eharmony.aloha.models.reg.RegressionFeatures
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenAggFuncAccessorProblems}
import spray.json.{JsonFormat, JsonReader}

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}


// TODO: When adding label-dep features, a Seq[GenAggFunc[K, Sparse]] will be needed.
// TODO: To create a Seq[GenAggFunc[K, Sparse]], a Semantics[K] needs to be derived from a Semantics[A].
// TODO: MorphableSemantics provides this.  If K is *embedded inside* A, it should be possible in some cases.
// TODO: An alternative is to pass a Map[K, Sparse], Map[K, Option[Sparse]], Map[K, Seq[Sparse]] or something.
// TODO: Directly passing the map of LDFs avoids the need to derive a Semantics[K].  This is easier to code.
// TODO: Directly passing LDFs would however be more burdensome to the data scientists.

/**
  * A multi-label predictor.
  *
  * Created by ryan.deak on 8/29/17.
  *
  * @param modelId An identifier for the model.  User in score and error reporting.
  * @param featureNames feature names (parallel to featureFunctions)
  * @param featureFunctions feature extracting functions.
  * @param labelsInTrainingSet a sequence of all labels encountered during training. Note: the
  *                            order of labels may relate to the predictor produced by
  *                            predictorProducer.  It is the caller's responsibility to ensure
  *                            the order is correct.  To mitigate such problems, both labels
  *                            and indices into labelsInTrainingSet are passed to the predictor
  *                            produced by predictorProducer.
  * @param labelsOfInterest if provided, a sequence of labels will be extracted from the example
  *                         for which a prediction is desired. The ''intersection'' of the
  *                         extracted labels and the training labels will be the labels for which
  *                         predictions will be produced.
  * @param predictorProducer the function produced when calling this function is responsible for
  *                          getting the data into the correct type and using it within an
  *                          underlying ML library to produce a prediction.  The mapping back to
  *                          (K, Double) pairs is also its responsibility.
  * @param numMissingThreshold if provided, we check whether the threshold is exceeded.  If so,
  *                            return an error instead of the computed score.  This is for missing
  *                            data situations.
  * @param auditor transforms a `Map[K, Double]` to a `B`.  Reports successes and errors.
  * @tparam U upper bound on model output type `B`
  * @tparam K type of label or class
  * @tparam A input type of the model
  * @tparam B output type of the model.
  */
case class MultilabelModel[U, K, -A, +B <: U](
    modelId: ModelIdentity,
    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[GenAggFunc[A, Sparse]],
    labelsInTrainingSet: sci.IndexedSeq[K],
    labelsOfInterest: Option[GenAggFunc[A, sci.IndexedSeq[K]]],
    predictorProducer: SparsePredictorProducer[K],
    numMissingThreshold: Option[Int],
    auditor: Auditor[U, Map[K, Double], B])
extends SubmodelBase[U, Map[K, Double], A, B]
   with RegressionFeatures[A] {

  import MultilabelModel._

  /**
    * predictory is transient lazy value because we don't need to worry about serialization.
    * We don't care about the lazy property.  It should be created eagerly.
    */
  @transient private[this] lazy val predictor = predictorProducer()
  predictor // Force predictor eagerly

  private[this] val labelToInd: Map[K, Int] =
    labelsInTrainingSet.zipWithIndex.map { case (label, i) => label -> i }(collection.breakOut)

  override def subvalue(a: A): Subvalue[B, Map[K, Double]] = {
    val li = labelsAndInfo(a, labelsInTrainingSet, labelsOfInterest, labelToInd)

    if (li.labels.isEmpty)
      reportNoPrediction(modelId, li, auditor)
    else {
      val Features(x, missing, missingOk) = constructFeatures(a)

      if (!missingOk)
        reportTooManyMissing(modelId, li, missing, auditor)
      else {
        // TODO: To support label-dependent features, fill last parameter with a valid value.
        val predictionTry = Try { predictor(x, li.labels, li.indices, sci.IndexedSeq.empty) }

        predictionTry match {
          case Success(pred) => reportSuccess(modelId, li, missing, pred, auditor)
          case Failure(ex)   => reportPredictorError(modelId, li, missing, ex, auditor)
        }
      }
    }
  }

  override def close(): Unit =
    predictor match {
      case closeable: Closeable => closeable.close()
      case _ =>
    }
}

object MultilabelModel extends ParserProviderCompanion {

  /**
    *
    * @param indices
    * @param labels
    * @param missingLabels
    * @param problems
    * @tparam K
    */
  protected[multilabel] case class LabelsAndInfo[K](
      indices: sci.IndexedSeq[Int],
      labels: sci.IndexedSeq[K],
      missingLabels: Seq[K],
      problems: Option[GenAggFuncAccessorProblems]
  )

  /**
    *
    * @param a
    * @param labelsInTrainingSet
    * @param labelsOfInterest
    * @param labelToInd
    * @tparam A
    * @tparam K
    * @return
    */
  protected[multilabel] def labelsAndInfo[A, K](
      a: A,
      labelsInTrainingSet: sci.IndexedSeq[K],
      labelsOfInterest: Option[GenAggFunc[A, sci.IndexedSeq[K]]],
      labelToInd: Map[K, Int]
  ): LabelsAndInfo[K] = {
    // TODO: Is this good enough?  Are we tracking enough missing information?  Probably not.
    labelsOfInterest.map ( labelFn =>
      labelsForPrediction(a, labelFn, labelToInd)
    ) getOrElse {
      LabelsAndInfo(labelsInTrainingSet.indices, labelsInTrainingSet, Seq.empty, None)
    }
  }

  /**
    *
    * @param modelId
    * @param missing
    * @param auditor
    * @tparam U
    * @tparam K
    * @tparam B
    * @return
    */
  protected[multilabel] def reportTooManyMissing[U, K, B](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      missing: scm.Map[String, Seq[String]],
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Nothing] = {
    // TODO: Fill in the errors.
    val aud = auditor.failure(modelId, missingVarNames = missing.values.flatten.toSet)
    Subvalue(aud, None)
  }

  /**
    *
    * @param modelId
    * @param labelInfo
    * @param auditor
    * @tparam U
    * @tparam K
    * @tparam B
    * @return
    */
  protected[multilabel] def reportNoPrediction[U, K, B](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Nothing] = {
    // TODO: Fill in the errors.
    val aud = auditor.failure(modelId)
    Subvalue(aud, None)
  }

  /**
    *
    * @param modelId
    * @param labelInfo
    * @param missing
    * @param prediction
    * @param auditor
    * @tparam U
    * @tparam K
    * @tparam B
    * @return
    */
  protected[multilabel] def reportSuccess[U, K, B](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      missing: scm.Map[String, Seq[String]],
      prediction: Map[K, Double],
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Map[K, Double]] = {

    val errors =
      if (labelInfo.missingLabels.nonEmpty)
        Seq(s"Labels provide for which a prediction could not be produced: ${labelInfo.missingLabels.mkString(", ")}.")
      else Seq.empty

    // TODO: Incorporate missing data reporting.
    val aud: B = auditor.success(modelId, prediction, errorMsgs = errors)

    Subvalue(aud, Option(prediction))
  }

  /**
    *
    * @param modelId
    * @param labelInfo
    * @param missing
    * @param throwable
    * @param auditor
    * @tparam U
    * @tparam K
    * @tparam B
    * @return
    */
  protected[multilabel] def reportPredictorError[U, K, B](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      missing: scm.Map[String, Seq[String]],
      throwable: Throwable,
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Nothing] = {

    // TODO: Fill in.
    val aud = auditor.failure(modelId)
    Subvalue(aud, None)
  }

  /**
    *
    * @param a
    * @param labelsOfInterest
    * @param labelToInd
    * @tparam A
    * @tparam K
    * @return
    */
  protected[multilabel] def labelsForPrediction[A, K](
      a: A,
      labelsOfInterest: GenAggFunc[A, sci.IndexedSeq[K]],
      labelToInd: Map[K, Int]
  ): LabelsAndInfo[K] = {

    val labelsShouldPredict = labelsOfInterest(a)

    val unsorted =
      for {
        label <- labelsShouldPredict
        ind   <- labelToInd.get(label).toList
      } yield (ind, label)

    val problems =
      if (labelsShouldPredict.nonEmpty) None
      else Option(labelsOfInterest.accessorOutputProblems(a))

    val noPrediction =
      if (unsorted.size == labelsShouldPredict.size) Seq.empty
      else labelsShouldPredict.filterNot(labelToInd.contains)

    val (ind, lab) = unsorted.sortBy{ case (i, _) => i }.unzip

    LabelsAndInfo(ind, lab, noPrediction, problems)
  }


  override def parser: ModelParser = Parser

  object Parser extends ModelSubmodelParsingPlugin {
    override val modelType: String = "multilabel-sparse"

    // TODO: Figure if a Option[JsonReader[MultilabelModel[U, _, A, B]]] can be returned.
    // See: parser that returns SegmentationModel[U, _, N, A, B]
    // See: parser that returns RegressionModel[U, A, B]
    // Seems like this should be possible but we get the error:
    //
    //   [error] method commonJsonReader has incompatible type
    //   [error]    override def commonJsonReader[U, N, A, B <: U](
    //   [error]                 ^
    //
    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])(implicit
        r: RefInfo[N],
        jf: JsonFormat[N]
    ): Option[JsonReader[_ <: Model[A, B] with Submodel[_, A, B]]] = {
      if (!RefInfoOps.isSubType[N, Map[_, Double]])
        None
      else {
        // Because N is a subtype of map, it "should" have two type parameters.
        // This is obviously not true in all cases, like with LongMap
        // http://scala-lang.org/files/archive/api/2.11.8/#scala.collection.immutable.LongMap
        // TODO: Make this more robust.
        val refInfoK = RefInfoOps.typeParams(r).head

        // To allow custom class (key) types, we'll need to create a custom ModelFactoryImpl instance
        // with a specialized RefInfoToJsonFormat.
        //
        // type: Option[JsonFormat[_]]
        val jsonFormatK = factory.jsonFormat(refInfoK)

        // TODO: parse the label extraction

        // TODO: parse the feature extraction

        // TODO: parse the native submodel from the wrapped ML library.  This involves plugins

        ???
      }
    }
  }
}
