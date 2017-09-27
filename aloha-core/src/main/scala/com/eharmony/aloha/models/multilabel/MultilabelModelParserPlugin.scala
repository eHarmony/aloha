package com.eharmony.aloha.models.multilabel

import com.eharmony.aloha.reflect.{RefInfo, RuntimeClasspathScanning}
import spray.json.{JsonFormat, JsonReader}

/**
  * A plugin that will produce the
  * Created by ryan.deak on 9/6/17.
  */
trait MultilabelModelParserPlugin {

  /**
    * A globally unique name for the plugin.  If there are name collisions,
    * [[MultilabelModelParserPlugin.plugins()]] will error out.
    * @return the plugin name.
    */
  def name: String

  /**
    * Provide a JSON reader that can translate JSON ASTs to a `SparsePredictorProducer`.
    * @param info information about the multi-label model passed to the plugin.
    * @param ri reflection information about the label type
    * @param jf a JSON format representing a bidirectional mapping between instances of
    *           `K` and JSON ASTs.
    * @tparam K the label or class type to be produced by the multi-label model.
    * @return a JSON reader that can create `SparsePredictorProducer[K]` from JSON ASTs.
    */
  def parser[K](info: PluginInfo[K])
               (implicit ri: RefInfo[K], jf: JsonFormat[K]): JsonReader[SparsePredictorProducer[K]]
}

object MultilabelModelParserPlugin extends RuntimeClasspathScanning {

  /**
    * Finds the plugins in the `com.eharmony.aloha` namespace.
    */
  // TODO: Consider making this implicit so that we can parameterize the parser implicitly on a sequence of plugins.
  protected[multilabel] def plugins(): Seq[MultilabelModelParserPlugin] =
    scanObjects[MultilabelPluginProviderCompanion, MultilabelModelParserPlugin]("multilabelPlugin")
}