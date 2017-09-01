package com.eharmony.aloha.models.multilabel

import java.io.Closeable

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models._
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import spray.json.{JsonFormat, JsonReader}

import scala.collection.{immutable => sci}


/**
  *
  * Created by ryan.deak on 8/29/17.
  *
  * @param modelId
  * @param labelsInTrainingSet
  * @param labelsOfInterest
  * @param featureNames
  * @param featureFunctions
  * @param predictorProducer
  * @param auditor
  * @tparam U upper bound on model output type `B`
  * @tparam K type of label or class
  * @tparam A input type of the model
  * @tparam B output type of the model.
  */
// TODO: When adding label-dep features, a Seq[GenAggFunc[K, Sparse]] will be needed.
// TODO: To create a Seq[GenAggFunc[K, Sparse]], a Semantics[K] needs to be derived from a Semantics[A].
// TODO: MorphableSemantics provides this.  If K is *embedded inside* A, it should be possible in some cases.
case class MultilabelModel[U, K, -A, +B <: U](
    modelId: ModelIdentity,
    labelsInTrainingSet: sci.IndexedSeq[K],

    labelsOfInterest: Option[GenAggFunc[A, sci.IndexedSeq[K]]],

    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[GenAggFunc[A, Sparse]],

    predictorProducer: SparsePredictorProducer[K],
    auditor: Auditor[U, Map[K, Double], B]
)
extends SubmodelBase[U, Map[K, Double], A, B] {
  import MultilabelModel._

  @transient private[this] lazy val predictor = predictorProducer()

  {
    // Force predictor eagerly
    predictor
  }

  private[this] val labelToInd: Map[K, Int] =
    labelsInTrainingSet.zipWithIndex.map { case (label, i) => label -> i }(collection.breakOut)


  override def subvalue(a: A): Subvalue[B, Map[K, Double]] = {

    // TODO: Is this good enough?  Are we tracking enough missing information?  Probably not.
    val (indices, labelsToPredict, labelsWithNoPrediction) =
      labelsOfInterest.map ( labelFn =>
        labelsForPrediction(a, labelFn, labelToInd)
      ) getOrElse {
        (labelsInTrainingSet.indices, labelsInTrainingSet, Seq.empty)
      }

    // TODO: Do a better job of tracking missing features like in the RegressionFeatures trait.
    val features = featureFunctions.map(f => f(a))

    // TODO: If labelsToPredict is empty, don't run predictor.  Return failure and report inside.

    // predictor is responsible for getting the data into the correct type and applying
    // it within the underlying ML library to produce a prediction.  The mapping back to
    // (K, Double) pairs is also its responsibility.
    //
    // TODO: When supporting label-dependent features, fill in the last parameter with a valid value.
    // TODO: Consider wrapping in a Try
    val natural = predictor(features, labelsToPredict, indices, sci.IndexedSeq.empty)

    val errors =
      if (labelsWithNoPrediction.nonEmpty)
        Seq(s"Labels provide for which a prediction could not be produced: ${labelsWithNoPrediction.mkString(", ")}.")
      else Seq.empty

    // TODO: Incorporate missing data reporting.
    val aud: B = auditor.success(modelId, natural, errorMsgs = errors)

    Subvalue(aud, Option(natural))
  }

  override def close(): Unit =
    predictor match {
      case closeable: Closeable => closeable.close()
      case _ =>
    }
}

object MultilabelModel extends ParserProviderCompanion {

  protected[multilabel] def labelsForPrediction[A, K](
      a: A,
      labelsOfInterest: GenAggFunc[A, sci.IndexedSeq[K]],
      labelToInd: Map[K, Int]
  ): (sci.IndexedSeq[Int], sci.IndexedSeq[K], Seq[K]) = {

    val labelsShouldPredict = labelsOfInterest(a)

    val unsorted =
      for {
        label <- labelsShouldPredict
        ind   <- labelToInd.get(label).toList
      } yield (ind, label)

    val noPrediction =
      if (unsorted.size == labelsShouldPredict.size) Seq.empty
      else labelsShouldPredict.filterNot(labelToInd.contains)

    val (ind, lab) = unsorted.sortBy{ case (i, _) => i }.unzip

    (ind, lab, noPrediction)
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
