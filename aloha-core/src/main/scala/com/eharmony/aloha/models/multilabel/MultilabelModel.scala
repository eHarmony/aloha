package com.eharmony.aloha.models.multilabel

import java.io.{Closeable, PrintWriter, StringWriter}

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.factory._
import com.eharmony.aloha.factory.ri2jf.RefInfoToJsonFormat
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models._
import com.eharmony.aloha.models.multilabel.json.MultilabelModelReader
import com.eharmony.aloha.models.reg.RegressionFeatures
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenAggFuncAccessorProblems}
import com.eharmony.aloha.util.{Logging, SerializabilityEvidence}
import spray.json.{JsonFormat, JsonReader}

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success}


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
  * @param modelId An identifier for the model.  Used in score and error reporting.
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
  *                          (K, Double) pairs is also its responsibility.  If the predictor
  *                          produced by predictorProducer is Closeable, it will be closed when
  *                          MultilabelModel's close method is called.
  * @param numMissingThreshold if provided, we check whether the threshold is exceeded.  If so,
  *                            return an error instead of the computed score.  This is for missing
  *                            data situations.
  * @param auditor transforms a `Map[K, Double]` to a `B`.  Reports successes and errors.
  * @param ev evidence that `K` is serializable.
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
(implicit ev: SerializabilityEvidence[K])
extends SubmodelBase[U, Map[K, Double], A, B]
   with RegressionFeatures[A] {

  import MultilabelModel._

  /**
    * predictory is transient lazy value because we don't need to worry about serialization.
    * We don't care about the lazy property.  It should be created eagerly.
    */
  @transient private[this] lazy val predictor = predictorProducer()
  predictor // Force predictor eagerly

  /**
    * Cache this in case labelsOfInterest is None.  In that case, we don't want to repeatedly
    * create this because it could create a GC burden for no real reason.
    */
  private[this] val defaultLabelInfo =
    // Hopefully, making this non-transient doesn't increase the serialized size much.
    // labelsInTrainingSet isn't serialized twice is it?
    LabelsAndInfo(labelsInTrainingSet.indices, labelsInTrainingSet, Seq.empty, None)

  /**
    * Making from label to index into the sequence of all labels encountered during training.
    */
  private[this] val labelToInd: Map[K, Int] =
    labelsInTrainingSet.zipWithIndex.map { case (label, i) => label -> i }(collection.breakOut)

  override def subvalue(a: A): Subvalue[B, Map[K, Double]] = {
    val li = labelsAndInfo(a, labelsOfInterest, labelToInd, defaultLabelInfo)

    if (li.labels.isEmpty)
      reportNoPrediction(modelId, li, auditor)
    else {
      val Features(x, missing, missingOk) = constructFeatures(a)

      if (!missingOk)
        reportTooManyMissing(modelId, li, missing, auditor)
      else {
        // TODO: To support label-dependent features, fill last parameter with a valid value.
        val predictionTry = predictor(x, li.labels, li.indices, sci.IndexedSeq.empty)

        predictionTry match {
          case Success(pred) => reportSuccess(modelId, li, missing, pred, auditor)
          case Failure(ex)   => reportPredictorError(modelId, li, missing, ex, auditor)
        }
      }
    }
  }

  /**
    * When the `predictor` passed to the constructor is a java.io.Closeable, its `close`
    * method is called.
    */
  override def close(): Unit =
    predictor match {
      case closeable: Closeable => closeable.close()
      case _ =>
    }
}

object MultilabelModel extends ParserProviderCompanion {

  /**
    * Contains information about the labels to be used for predictions, and problems encountered
    * while trying to get those labels.
    * @param indices indices into the sequence of all labels seen during training.  These should
    *                be sorted in ascending order.
    * @param labels labels for which a prediction should be produced.  labels are parallel to
    *               indices so `indices(i)` is the index associated with `labels(i)`.
    * @param labelsNotInTrainingSet a sequence of labels derived from the input data that could
    *                               not be found in the sequence of all labels seen during training.
    * @param problems any problems encountered when trying to get the labels.  This should only
    *                 be present when the caller indicates labels should be embedded in the
    *                 input data passed to the prediction function in the MultilabelModel.
    * @tparam K type of label or class
    */
  protected[multilabel] case class LabelsAndInfo[K](
      indices: sci.IndexedSeq[Int],
      labels: sci.IndexedSeq[K],
      labelsNotInTrainingSet: Seq[K],
      problems: Option[GenAggFuncAccessorProblems]
  ) {
    def missingVarNames: Seq[String] = problems.map(p => p.missing).getOrElse(Nil)
    def errorMsgs: Seq[String] = {
      labelsNotInTrainingSet.map { lab => s"Label not in training labels: $lab" } ++
      problems.map(p => p.errors).getOrElse(Nil)
    }
  }

  private[multilabel] val NumLinesToKeepInStackTrace = 20

  private[multilabel] val TooManyMissingError =
    "Too many missing features encountered to produce prediction."

  private[multilabel] val NoLabelsError = "No labels provided. Cannot produce a prediction."

  /**
    * Get the labels and information about the labels.
    * @param a an input from which label information should be derived if labelsOfInterest is not empty.
    * @param labelsOfInterest an optional function used to extract label information from the input `a`.
    * @param labelToInd a mapping from label to index into the sequence of all labels seen during training.
    * @param defaultLabelInfo label information related to all labels seen at training time.  If
    *                         `labelsOfInterest` is not provided, this information will be used.
    * @tparam A input type of the model
    * @tparam K type of label or class
    * @return labels and information about the labels.
    */
  protected[multilabel] def labelsAndInfo[A, K](
      a: A,
      labelsOfInterest: Option[GenAggFunc[A, sci.IndexedSeq[K]]],
      labelToInd: Map[K, Int],
      defaultLabelInfo: LabelsAndInfo[K]
  ): LabelsAndInfo[K] =
    labelsOfInterest.fold(defaultLabelInfo)(f => labelsForPrediction(a, f, labelToInd))

  /**
    * Combine the missing variables found into a set.
    * @param labelInfo labels and information about the labels.
    * @param missing missing features from
    * @tparam K type of label or class
    * @return a set of missing features
    */
  protected[multilabel] def combineMissing[K](
      labelInfo: LabelsAndInfo[K],
      missing: scm.Map[String, Seq[String]]
  ): Set[String] = missing.values.flatten.toSet ++ labelInfo.missingVarNames

  /**
    * Report that a prediction could not be made because too many missing features were encountered.
    * @param modelId An identifier for the model.  Used in error reporting.
    * @param labelInfo labels and information about the labels.
    * @param missing missing features from
    * @param auditor an auditor used to audit the output.
    * @tparam U upper bound on model output type `B`
    * @tparam K type of label or class
    * @tparam B output type of the model.
    * @return a SubValue indicating failure.
    */
  protected[multilabel] def reportTooManyMissing[U, K, B <: U](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      missing: scm.Map[String, Seq[String]],
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Nothing] = {

    // TODO: Check that missing.values.flatten.toSet AND labelInfo.missingFeatures have the same format.
    val aud = auditor.failure(
      modelId,
      errorMsgs = TooManyMissingError +: labelInfo.errorMsgs,
      missingVarNames = combineMissing(labelInfo, missing)
    )
    Subvalue(aud, None)
  }

  /**
    * Report that no prediction attempt was made because of issues with the labels.
    * @param modelId An identifier for the model.  Used in error reporting.
    * @param labelInfo labels and information about the labels.
    * @param auditor an auditor used to audit the output.
    * @tparam U upper bound on model output type `B`
    * @tparam K type of label or class
    * @tparam B output type of the model.
    * @return a SubValue indicating failure.
    */
  protected[multilabel] def reportNoPrediction[U, K, B <: U](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Nothing] = {
    val aud = auditor.failure(
      modelId,
      errorMsgs = NoLabelsError +: labelInfo.errorMsgs,
      missingVarNames = labelInfo.missingVarNames.toSet
    )
    Subvalue(aud, None)
  }

  /**
    * Report that the model succeeded.
    * @param modelId An identifier for the model.  Used in score reporting.
    * @param labelInfo labels and information about the labels.
    * @param missing missing features from
    * @param prediction the prediction(s) made by the embedded predictor.
    * @param auditor an auditor used to audit the output.
    * @tparam U upper bound on model output type `B`
    * @tparam K type of label or class
    * @tparam B output type of the model.
    * @return a SubValue indicating success.
    */
  protected[multilabel] def reportSuccess[U, K, B <: U](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      missing: scm.Map[String, Seq[String]],
      prediction: Map[K, Double],
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Map[K, Double]] = {

    val aud = auditor.success(
      modelId,
      prediction,
      errorMsgs = labelInfo.errorMsgs,
      missingVarNames = combineMissing(labelInfo, missing)
    )

    Subvalue(aud, Option(prediction))
  }

  /**
    * Report that a `Throwable` was thrown while invoking the predictor
    * @param modelId An identifier for the model.  Used in error reporting.
    * @param labelInfo labels and information about the labels.
    * @param missingFeatureMap missing features from RegressionFeatures
    * @param throwable the error the occurred in the predictor.
    * @param auditor an auditor used to audit the output.
    * @tparam U upper bound on model output type `B`
    * @tparam K type of label or class
    * @tparam B output type of the model.
    * @return a SubValue indicating failure.
    */
  protected[multilabel] def reportPredictorError[U, K, B <: U](
      modelId: ModelIdentity,
      labelInfo: LabelsAndInfo[K],
      missingFeatureMap: scm.Map[String, Seq[String]],
      throwable: Throwable,
      auditor: Auditor[U, Map[K, Double], B]
  ): Subvalue[B, Nothing] = {

    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    throwable.printStackTrace(pw)
    val stackTrace = sw.toString.split("\n").take(NumLinesToKeepInStackTrace).mkString("\n")

    val aud = auditor.failure(
      modelId,
      errorMsgs = stackTrace +: labelInfo.errorMsgs,
      missingVarNames = combineMissing(labelInfo, missingFeatureMap)
    )
    Subvalue(aud, None)
  }

  /**
    * Get labels from the input for which a prediction should be produced.
    * @param example the example provided to the model
    * @param labelsOfInterest a function used to extract labels for which a
    *                         prediction should be produced.
    * @param labelToInd mapping from Label to index into the sequence of all
    *                   labels seen in the training set.
    * @tparam A input type of the model
    * @tparam K type of label or class
    * @return labels and information about the labels.
    */
  protected[multilabel] def labelsForPrediction[A, K](
      example: A,
      labelsOfInterest: GenAggFunc[A, sci.IndexedSeq[K]],
      labelToInd: Map[K, Int]
  ): LabelsAndInfo[K] = {

    val labelsShouldPredict = labelsOfInterest(example)

    val unsorted =
      for {
        label <- labelsShouldPredict
        ind   <- labelToInd.get(label).toList
      } yield (ind, label)

    val problems =
      if (labelsShouldPredict.nonEmpty) None
      else Option(labelsOfInterest.accessorOutputProblems(example))

    val noPrediction =
      if (unsorted.size == labelsShouldPredict.size) Seq.empty
      else labelsShouldPredict.filterNot(labelToInd.contains)

    val (ind, lab) = unsorted.sortBy{ case (i, _) => i }.unzip

    LabelsAndInfo(ind, lab, noPrediction, problems)
  }

  private[multilabel] lazy val plugins: Map[String, MultilabelModelParserPlugin] =
    MultilabelModelParserPlugin.plugins().map(p => p.name -> p).toMap

  override def parser: ModelParser = Parser

  object Parser extends ModelSubmodelParsingPlugin with Logging {
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
    ): Option[JsonReader[Model[A, B] with Submodel[N, A, B]]] = {

      // If the type N is a Map with Double values, we can specify a key type (call it K).
      // Then the necessary type classes related to K are *instantiated* and a reader is
      // created using types N and K.  Ultimately, the reader consumes the type `K` but
      // only the type N is exposed in the returned reader.
      if (!RefInfoOps.isSubType[N, Map[_, Double]]) {
        warn(s"N=${RefInfoOps.toString[N]} is not a Map[K, Double]. Cannot create a JsonReader for MultilabelModel.")
        None
      }
      else if (2 != RefInfoOps.typeParams(r).size) {
        warn(s"N=${RefInfoOps.toString[N]} does not have 2 type parameters. Cannot infer label type K needed create a JsonReader for MultilabelModel.")
        None
      }
      else {
        // This would be a prime candidate for the WriterT monad transformer:
        // type Result[A] = WriterT[Option, Vector[String], A]
        // https://github.com/typelevel/cats/blob/0.7.x/core/src/main/scala/cats/data/WriterT.scala#L8
        val readerAttempt =
          for {
            ri <- refInfoOrError[N, Any](r).right    // Force type of K = Any
            jf <- jsonFormatOrError(factory, ri).right
            se <- serializableEvidenceOrError(ri).right
          } yield reader(semantics, auditor, ri, jf, se)

        readerAttempt match {
          case Left(err) =>
            warn(err)
            None
          case Right(reader) => Option(reader)
        }
      }
    }
  }

  /**
    * Produce the reader.
    *
    * '''NOTE''': This function should only be applied after we know `N` equals `Map[K, Double]`
    * for some `K`.
    *
    * @param semantics semantics to use for compiling specifications.
    * @param auditor an auditor of type `Auditor[U, N, B]`.  It's not of type
    *                `Auditor[U, Map[K, Double], B]`, but because we know the relationship
    *                between `N` and `Map[K, Double]`, we can cast `N` to `Map[K, Double]`.
    * @param ri reflection information about `K`.
    * @param jf a JSON format that can translate JSON ASTs to and from `K`s.
    * @param se evidence that `K` is `Serializable`.
    * @tparam U upper bound on model output type `B`
    * @tparam N the expected natural output type the model that will be produced by the
    *           JSON reader.  This should be isomorphic to `Map[K, Double]`.
    * @tparam K type of label or class
    * @tparam A input type of the model.
    * @tparam B output type of the model.
    * @return a JSON reader capable of producing a [[MultilabelModel]] from a JSON definition.
    */
  private[multilabel] def reader[U, N, K, A, B <: U](
      semantics: Semantics[A],
      auditor: Auditor[U, N, B],
      ri: RefInfo[K],
      jf: JsonFormat[K],
      se: SerializabilityEvidence[K]): JsonReader[Model[A, B] with Submodel[N, A, B]] = {
    // At this point, N = Map[K, Double], so we are just casting to itself essentially.
    val aud = auditor.asInstanceOf[Auditor[U, Map[K, Double], B]]

    // Create a cast from Map[K, Double] to N.  Ideally, this would be a Map[K, Double] <:< N
    // rather than a Map[K, Double] => N.  But it's hard (with good reason) to create a <:<
    // and easy to create a function.
    implicit val cast = (m: Map[K, Double]) => m.asInstanceOf[N]

    // MultilabelModelReader can produce a MultilabelModel.  But there's a problem returning
    // the proper type because the compiler doesn't have compile-time evidence that N is
    // Map[K, Double] so, a less specific type (Model[A, B] with Submodel[N, A, B]) is
    // returned.
    MultilabelModelReader(semantics, aud, plugins)(ri, jf, se).untypedReader[N]
  }

  private[multilabel] def serializableEvidence[K](refInfoK: RefInfo[K]) = {
    val serEv =
      if (RefInfoOps.isSubType(refInfoK, RefInfo.JavaSerializable))
        Option(SerializabilityEvidence.serializableEvidence[java.io.Serializable])
      else if (RefInfoOps.isSubType(refInfoK, RefInfo.AnyVal))
        Option(SerializabilityEvidence.anyValEvidence[AnyVal])
      else None

    serEv.asInstanceOf[Option[SerializabilityEvidence[K]]]
  }

  private[multilabel] def serializableEvidenceOrError[K](refInfoK: RefInfo[K]) = {
    serializableEvidence(refInfoK)
      .toRight(s"Couldn't produce evidence that ${RefInfoOps.toString(refInfoK)} is Serializable.")
  }

  /**
    * Get reflection information about the label type `K` for the [[MultilabelModel]] to be produced.
    *
    * At the time of application, we should already know N is a subtype of Map with 2 type parameters.
    * This will preclude things like
    * [[http://scala-lang.org/files/archive/api/2.11.8/#scala.collection.immutable.LongMap scala.collection.immutable.LongMap]].
    * @param rin reflection information about `N`.
    * @tparam N natural output type of the model. `N` should equal `Map[K, Double]`.
    * @tparam K label type of the [[MultilabelModel]]
    * @return reflection information about K.
    */
  private[multilabel] def refInfo[N, K](rin: RefInfo[N]) =
    RefInfoOps.typeParams(rin).headOption.asInstanceOf[Option[RefInfo[K]]]

  private[multilabel] def refInfoOrError[N, K](rin: RefInfo[N]) = {
    refInfo[N, K](rin)
      .toRight(s"Couldn't extract key type from natural type: ${RefInfoOps.toString(rin)}")
  }

  private[multilabel] def jsonFormatOrError[U, A, K](factory: SubmodelFactory[U, A], refInfoK: RefInfo[K]): Either[String, JsonFormat[K]] = {
    // To allow custom class (key) types, we'll need to create a custom ModelFactoryImpl instance
    // with a specialized RefInfoToJsonFormat.
    factory.jsonFormat(refInfoK)
      .toRight(s"Couldn't find a JSON Format for ${RefInfoOps.toString(refInfoK)}.  Consider using a different ${classOf[RefInfoToJsonFormat].getCanonicalName}.")
  }
}
