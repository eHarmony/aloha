package com.eharmony.aloha.models

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.multilabel.SparsePredictorProducer
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
  * @param labelInTrainingSet
  * @param labelsOfInterest
  * @param featureNames
  * @param featureFunctions
  * @param labelDependentFeatures
  * @param predictorProducer
  * @param auditor
  * @tparam U upper bound on model output type `B`
  * @tparam K type of label or class
  * @tparam A input type of the model
  * @tparam B output type of the model.
  */
case class MultilabelModel[U, K, -A, +B <: U](
    modelId: ModelIdentity,
    labelInTrainingSet: Seq[K],

    labelsOfInterest: Option[GenAggFunc[A, sci.IndexedSeq[K]]],

    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[GenAggFunc[A, Sparse]],

    // Here is a problem.  B/C we only have a Semantics[A] (and not a Semantics[K]) in the factory,
    // we can't produce sci.IndexedSeq[GenAggFunc[K, Sparse]] representing the many feature functions
    // that will be applied to each label to produce the sparse values.  We CAN produce a
    // sci.IndexedSeq[GenAggFunc[A, sci.IndexedSeq[Sparse]]] where sci.IndexedSeq[Sparse] is the result
    // of one feature applied to all applicable labels.  Many feature definitions
    // are possible and that's why there is a sci.IndexedSeq[GenAggFunc[...]].  But the issue arises
    // of how to link the indices of sci.IndexedSeq[Sparse] to the indices of labelsOfInterest,
    // especially if labelsOfInterest is an Option and not required.
    //
    // We can have an "honor system" based approach where we kindly ask the caller to behave and make
    // the sequences line up, but that always seems like a recipe for disaster.
    //
    // We could do sci.IndexedSeq[GenAggFunc[A, sci.IndexedSeq[(K, Sparse)]]] and remove the honor
    // system approach but that seems like a lot of computational overhead.
    //
    // It would be nice if we could use MorphableSemantics to change Semantics[A] into Semantics[K]
    // but this is fraught with problems.  For instance, with the Avro semantics, we have to supply
    // parameters to create a CompiledSemanticsAvroPlugin.  Therefore, we'd have to use reflection
    // to see how K relates to A, and it's not guaranteed that K is embedded in A and that K is a
    // GenericRecord.
    //
    labelDependentFeatures: sci.IndexedSeq[GenAggFunc[A, sci.IndexedSeq[Sparse]]],

    predictorProducer: SparsePredictorProducer[K],
    auditor: Auditor[U, Map[K, Double], B]
)
extends SubmodelBase[U, Map[K, Double], A, B] {

  @transient private[this] lazy val predictor = predictorProducer()

  {
    // Force predictor eagerly
    predictor
  }

  // TODO: Create the label dependent features once and then select them based on the labels created from the example.
  // This is going to be very difficult when the label dependent feature functions are produced by the factory
  // because we don't have a Semantics[K], we only have a semantics[A].  So, if we want to cut down on computation
  // time, we can cache the features using a scala.collection.concurrent.TrieMap or a
  // java.util.concurrent.ConcurrentHashMap.  This should be fine AS LONG AS the functions in labelDependentFeatures
  // are idempotent.  That should be a stated in the documentation for that parameter.

  private val globalLdf = labelExtraction match {
    case KnownLabelExtraction(labels) => Option(applyLdf(labels))
    case _ => None
  }

  private[this] def applyLdf(labels: Seq[K]): Seq[sci.IndexedSeq[F]] =
    labels.map(label => labelDependentFeatures.map(f => f(label)))

  override def subvalue(a: A): Subvalue[B, Map[K, Double]] = {

    // Get the labels for which predictions should be produced.
    val labels = labelExtraction match {
      case KnownLabelExtraction(constLabels) => constLabels
      case PerExampleLabelExtraction(extractor) => extractor(a)
    }

    // Get the label-dependent features.
    // TODO: Handle missing data in the extraction process like in RegressionFeatures.constructFeatures
    val ldf: Seq[sci.IndexedSeq[F]] = globalLdf getOrElse applyLdf(labels)

    // TODO: Handle missing data in the extraction process like in RegressionFeatures.constructFeatures
    val features: sci.IndexedSeq[F] = featureExtraction.map(f => f(a))

    // predictor is responsible for getting the data into the correct type and applying
    // it within the underlying ML library to produce a prediction.  The mapping back to
    // (K, Double) pairs is also its responsibility.
    val natural: Map[K, Double] = predictor(features, labels, ldf)
    val aud: B = auditor.success(modelId, natural)
    Subvalue(aud, Option(natural))
  }
}

object MultilabelModel extends ParserProviderCompanion {

  object Parser extends ModelParsingPlugin {
    override val modelType: String = "multilabel"
    override def modelJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B]
    )(implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[MultilabelModel[U, _, _, A, B]]] = {

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

  override def parser: ModelParser = Parser
}
