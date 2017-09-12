package com.eharmony.aloha.models.vw.jni.multilabel.json

import com.eharmony.aloha.models.multilabel.SparsePredictorProducer
import com.eharmony.aloha.models.vw.jni.Namespaces
import com.eharmony.aloha.models.vw.jni.multilabel.VwSparseMultilabelPredictorProducer
import com.eharmony.aloha.util.Logging
import spray.json.{JsValue, JsonReader}

import scala.collection.immutable.ListMap

/**
  *
  *
  * '''NOTE''': This class extends `JsonReader[SparsePredictorProducer[K]]` rather than the
  * more specific type `JsonReader[VwSparseMultilabelPredictorProducer[K]]` because `JsonReader`
  * is not covariant in its type parameter.
  *
  * Created by ryan.deak on 9/8/17.
  *
  * @param featureNames feature names from the multi-label model.
  * @tparam K label type for the predictions outputted by the
  */
case class VwMultilabelModelPluginJsonReader[K](featureNames: Seq[String])
   extends JsonReader[SparsePredictorProducer[K]]
      with VwMultilabelModelJson
      with Namespaces
      with Logging {

  import VwMultilabelModelPluginJsonReader._

  override def read(json: JsValue): VwSparseMultilabelPredictorProducer[K] = {
    val ast = json.asJsObject(notObjErr(json)).convertTo[VwMultilabelAst]
    val params = vwParams(ast.params)
    val (namespaces, defaultNs, missing) =
      allNamespaceIndices(featureNames, ast.namespaces.getOrElse(ListMap.empty))

    if (missing.nonEmpty)
      info(s"features in namespaces not found in featureNames: $missing")

    VwSparseMultilabelPredictorProducer[K](ast.modelSource, params, defaultNs, namespaces)
  }
}

object VwMultilabelModelPluginJsonReader extends Logging {
  private val JsonErrStrLength = 100

  private[multilabel] def vwParams(params: Either[Seq[String], String]): String =
    params.fold(_ mkString " ", identity).trim

  private[multilabel] def notObjErr(json: JsValue): String = {
    val str = json.prettyPrint
    val substr = str.substring(0, JsonErrStrLength)
    s"JSON object expected.  Found " + substr + (if (str.length != substr.length) " ..." else "")
  }
}