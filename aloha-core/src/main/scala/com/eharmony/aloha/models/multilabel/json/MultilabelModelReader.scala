package com.eharmony.aloha.models.multilabel.json

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.models.{Model, Submodel}
import com.eharmony.aloha.models.multilabel.{MultilabelModel, MultilabelModelParserPlugin}
import com.eharmony.aloha.models.reg.RegFeatureCompiler
import com.eharmony.aloha.models.reg.json.Spec
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, SerializabilityEvidence}
import spray.json.{DeserializationException, JsValue, JsonFormat, JsonReader}

import scala.collection.{immutable => sci}

/**
  * A JSON reader capable of turning JSON to a [[MultilabelModel]].
  * Created by ryan.deak on 9/7/17.
  * @param semantics semantics used to generate features and labels of interest.
  * @param auditor the auditor used to translate the [[MultilabelModel]] output values to `B` instances.
  * @param plugins the possible plugins to which [[MultilabelModel]] can delegate to produce predictions.
  *                Plugins are responsible for creating the `predictorProducer` passed to the
  *                [[MultilabelModel]] constructor.
  * @param refInfoK reflection information about the label type.
  * @param jsonFormatK a JSON format capable of parsing the label type.
  * @param serEvK evidence that the label type is `Serializable`.
  * @tparam U upper bound on model output type `B`
  * @tparam K type of label or class
  * @tparam A input type of the model
  * @tparam B output type of the model.
  */
final case class MultilabelModelReader[U, K, A, B <: U](
    semantics: Semantics[A],
    auditor: Auditor[U, Map[K, Double], B],
    plugins: Map[String, MultilabelModelParserPlugin])
(implicit
    refInfoK: RefInfo[K],
    jsonFormatK: JsonFormat[K],
    serEvK: SerializabilityEvidence[K])
   extends JsonReader[MultilabelModel[U, K, A, B]]
      with MultilabelModelJson
      with EitherHelpers
      with RegFeatureCompiler { self =>

  /**
    * Creates a reader for [[MultilabelModel]] that has a less specific type assuming that we
    * can produce evidence that `N` is a super type of `Map[K, Double]`.
    * @param ev Provides a conversion from `Map[K, Double]` to `N`.  This indicates a subtype
    *           relationship and is ''close to the same'' as implicit subtype evidence
    *           `<:<`, but is not as strong (because it's not statically checked by the compiler).
    * @tparam N The natural output of the Model that is a super type of `Map[K, Double]`.  Since
    *           auditors take values of type `N` as input, supplying a subtype should also work.
    * @return A JSON reader the creates [[MultilabelModel]], but returns instances as a less
    *         specific type.
    */
  private[multilabel] def untypedReader[N](implicit ev: Map[K, Double] => N): JsonReader[Model[A, B] with Submodel[N, A, B]] =
    new JsonReader[Model[A, B] with Submodel[N, A, B]]{
      override def read(json: JsValue): Model[A, B] with Submodel[N, A, B] =
        self.read(json).asInstanceOf[Model[A, B] with Submodel[N, A, B]]
    }

  override def read(json: JsValue): MultilabelModel[U, K, A, B] = {
    val mlData = json.convertTo[MultilabelData[K]]
    val pluginType = mlData.underlying.convertTo[Plugin].`type`

    plugins.get(pluginType)
      .map { plugin => model(mlData, plugin) }
      .getOrElse { throw new DeserializationException(errorMsg(pluginType)) }
  }

  private[multilabel] def errorMsg(pluginType: String): String = {
    val pluginNames = plugins.mapValues(p => p.getClass.getCanonicalName)
    s"Couldn't find plugin for type $pluginType.  Plugins available: $pluginNames"
  }

  private[multilabel] def compileLabelsOfInterest(labelsOfInterestSpec: String): ENS[GenAggFunc[A, sci.IndexedSeq[K]]] = {
    semantics.createFunction[sci.IndexedSeq[K]](labelsOfInterestSpec, Option(Vector.empty[K])).
      left.map { Seq(s"Error processing spec '$labelsOfInterestSpec'") ++ _ }
  }

  private[multilabel] def model(mlData: MultilabelData[K], plugin: MultilabelModelParserPlugin): MultilabelModel[U, K, A, B] = {
    val predProdReader = plugin.parser(mlData)(refInfoK, jsonFormatK)
    val predProd = mlData.underlying.convertTo(predProdReader)

    // Compile the labels of interest function.
    val labelsOfInterest = mlData.labelsOfInterest.map { spec =>
      compileLabelsOfInterest(spec) match {
        case Left(errs) => throw new DeserializationException(errs.mkString("\n"))
        case Right(f)   => f
      }
    }

    // Compile the features.
    val featureMap: Seq[(String, Spec)] = mlData.features.toSeq
    val (featureNames, featureFns) =
      features(featureMap, semantics)
        .fold(f => throw new DeserializationException(f.mkString("\n")), identity)
        .toIndexedSeq.unzip

    MultilabelModel(
      modelId = mlData.modelId,
      featureNames = featureNames,
      featureFunctions = featureFns,
      labelsInTrainingSet = mlData.labelsInTrainingSet,
      labelsOfInterest = labelsOfInterest,
      predictorProducer = predProd,
      numMissingThreshold = mlData.numMissingThreshold,
      auditor = auditor
    )(serEvK)
  }
}