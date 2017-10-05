package com.eharmony.aloha.models.vw.jni.multilabel

import com.eharmony.aloha.dataset.json.{Namespace, SparseSpec}
import com.eharmony.aloha.dataset.vw.multilabel.json.VwMultilabeledJson
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.sources.{Base64StringSource, ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.multilabel.MultilabelModel
import com.eharmony.aloha.models.multilabel.json.MultilabelModelJson
import com.eharmony.aloha.models.reg.json.Spec
import com.eharmony.aloha.models.vw.jni.VwJniModel
import com.eharmony.aloha.models.vw.jni.multilabel.json.VwMultilabelModelJson
import spray.json.{DefaultJsonProtocol, JsValue, JsonWriter, pimpAny, pimpString}

import scala.collection.immutable.ListMap

/**
  * Created by ryan.deak on 10/5/17.
  */
private[multilabel] trait VwMultlabelJsonCreator
extends MultilabelModelJson
   with VwMultilabelModelJson {

  /**
    * Create a JSON representation of an Aloha model.
    *
    * '''NOTE''': Because of the inclusion of the unrestricted `labelsOfInterest` parameter,
    *             the JSON produced by this function is not guaranteed to result in a valid
    *             Aloha model.  This is because no semantics are required by this function and
    *             so, the `labelsOfInterest` function specification cannot be validated.
    *
    * @param datasetSpec a location of a dataset specification.
    * @param binaryVwModel a location of a VW binary model file.
    * @param id a model ID.
    * @param labelsInTrainingSet The sequence of all labels encountered in the training set used
    *                            to produce the `binaryVwModel`.
    *                            '''''It is extremely important that this sequence has the
    *                                 same order as the sequence of labels used in the
    *                                 dataset creation process.'''''  Otherwise, the VW model might
    *                                 associate scores with an incorrect label.
    * @param labelsOfInterest It is possible that a model is trained on a super set of labels for
    *                         which predictions can be made.  If the labels at prediction time
    *                         differs (''or should be extracted from the input to the model''),
    *                         this function can provide that capability.
    * @param vwArgs arguments that should be passed to the VW model.  This likely isn't strictly
    *               necessary.
    * @param externalModel whether the underlying binary VW model should remain as a separate
    *                      file and be referenced by the Aloha model specification (`true`)
    *                      or the binary model content should be embeeded directly into the model
    *                      (`false`).  '''Keep in mind''' Aloha models must be smaller than 2 GB
    *                      because they are decoded to `String`s and `String`s are indexed by
    *                      32-bit integers (which have a max value of 2^32^ - 1).
    * @param numMissingThreshold the number of missing features to tolerate before emitting a
    *                            prediction failure.
    * @tparam K the type of label or class.
    * @return a JSON object.
    */
  def json[K: JsonWriter](
                           datasetSpec: Vfs,
                           binaryVwModel: Vfs,
                           id: ModelIdentity,
                           labelsInTrainingSet: Seq[K],
                           labelsOfInterest: Option[String] = None,
                           vwArgs: Option[String] = None,
                           externalModel: Boolean = false,
                           numMissingThreshold: Option[Int] = None
                         ): JsValue = {

    val dsJsAst = StringReadable.fromInputStream(datasetSpec.inputStream).parseJson
    val ds = dsJsAst.convertTo[VwMultilabeledJson]
    val features = modelFeatures(ds.features)
    val namespaces = ds.namespaces.map(modelNamespaces)
    val modelSrc = modelSource(binaryVwModel, externalModel)
    val vw = vwModelPlugin(modelSrc, vwArgs, namespaces)
    val model = modelAst(id, features, numMissingThreshold, labelsInTrainingSet, labelsOfInterest, vw)

    // Even though we are only *writing* JSON, we need a JsonFormat[K], (which is a reader
    // and writer) because multilabelDataJsonFormat has a JsonFormat context bound on K.
    // So to turn MultilabelData into JSON, we need to lift the JsonWriter for K into a
    // JsonFormat and store it as an implicit value (or create an implicit conversion
    // function on implicit arguments.
    implicit val labelJF = DefaultJsonProtocol.lift(implicitly[JsonWriter[K]])
    model.toJson
  }

  private[multilabel] def modelFeatures(featuresSpecs: Seq[SparseSpec]) =
    ListMap (
      featuresSpecs.map { case SparseSpec(name, spec, defVal) =>
        name -> Spec(spec, defVal.map(ts => ts.toSeq))
      }: _*
    )

  private[multilabel] def modelNamespaces(nss: Seq[Namespace]) =
    ListMap(
      nss.map(ns => ns.name -> ns.features) : _*
    )

  private[multilabel] def modelSource(binaryVwModel: Vfs, externalModel: Boolean) =
    if (externalModel)
      ExternalSource(binaryVwModel)
    else Base64StringSource(VwJniModel.readBinaryVwModelToB64String(binaryVwModel.inputStream))

  // Private b/c VwMultilabelAst is protected[this].  Don't let it escape.
  private def vwModelPlugin(
                             modelSrc: ModelSource,
                             vwArgs: Option[String],
                             namespaces: Option[ListMap[String, Seq[String]]]) =
    VwMultilabelAst(
      VwSparseMultilabelPredictorProducer.multilabelPlugin.name,
      modelSrc,
      vwArgs.map(a => Right(a)),
      namespaces
    )

  // Private b/c MultilabelData is private[this].  Don't let it escape.
  private def modelAst[K](
                           id: ModelIdentity,
                           features: ListMap[String, Spec],
                           numMissingThreshold: Option[Int],
                           labelsInTrainingSet: Seq[K],
                           labelsOfInterest: Option[String],
                           vwPlugin: VwMultilabelAst) =
    MultilabelData(
      modelType = MultilabelModel.parser.modelType,
      modelId = ModelId(id.getId(), id.getName()),
      features = features,
      numMissingThreshold = numMissingThreshold,
      labelsInTrainingSet = labelsInTrainingSet.toVector,
      labelsOfInterest = labelsOfInterest,
      underlying = vwPlugin.toJson.asJsObject
    )
}
