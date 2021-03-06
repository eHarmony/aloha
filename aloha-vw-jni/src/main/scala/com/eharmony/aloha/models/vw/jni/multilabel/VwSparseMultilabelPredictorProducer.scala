package com.eharmony.aloha.models.vw.jni.multilabel

import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.multilabel._
import com.eharmony.aloha.models.vw.jni.multilabel.json.VwMultilabelModelPluginJsonReader
import com.eharmony.aloha.reflect.RefInfo
import spray.json.{JsonFormat, JsonReader}

/**
  * A wrapper responsible for creating a [[VwSparseMultilabelPredictor]].  This defers
  * creation since VW JNI models are not Serializable.  This is because they are
  * thin wrappers around native C models in memory.  The underlying binary model is
  * externalizable in a file, byte array, etc. and this information is contained in
  * `modelSource`.  The actual VW JNI model is created only where needed.  In short,
  * this class can be serialized but the JNI model created from `modelSource` cannot
  * be.
  *
  * @param modelSource a source from which the binary VW model information can be
  *                    extracted and used to create a VW JNI model.
  * @param defaultNs indices into the features that belong to VW's default namespace.
  * @param namespaces namespace name to feature indices map.
  * @tparam K type of the labels returned by the [[VwSparseMultilabelPredictor]] that
  *           will be produced.
  * @author deaktator
  * @since 9/5/2017
  */
case class VwSparseMultilabelPredictorProducer[K](
    modelSource: ModelSource,
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    labelNamespace: Char,
    numLabelsInTrainingSet: Int
) extends SparsePredictorProducer[K] {
  override def apply(): VwSparseMultilabelPredictor[K] =
    VwSparseMultilabelPredictor[K](modelSource, defaultNs, namespaces, numLabelsInTrainingSet)
}

object VwSparseMultilabelPredictorProducer extends MultilabelPluginProviderCompanion {
  def multilabelPlugin: MultilabelModelParserPlugin = Plugin

  object Plugin extends MultilabelModelParserPlugin {
    override def name: String = "vw"

    override def parser[K](info: PluginInfo[K])
                          (implicit ri: RefInfo[K], jf: JsonFormat[K]): JsonReader[SparsePredictorProducer[K]] = {
      VwMultilabelModelPluginJsonReader[K](info.features.keys.toVector, info.labelsInTrainingSet.size)
    }
  }
}
