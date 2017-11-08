package com.eharmony.aloha.models.vw.jni.multilabel.json

import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator.{LabelNamespaces, determineLabelNamespaces}
import com.eharmony.aloha.models.multilabel.SparsePredictorProducer
import com.eharmony.aloha.models.vw.jni.Namespaces
import com.eharmony.aloha.models.vw.jni.multilabel.VwSparseMultilabelPredictorProducer
import com.eharmony.aloha.util.Logging
import spray.json.{DeserializationException, JsValue, JsonReader}

import scala.collection.breakOut
import scala.collection.immutable.ListMap

/**
  * A JSON reader for SparsePredictorProducer.
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
case class VwMultilabelModelPluginJsonReader[K](featureNames: Seq[String], numLabelsInTrainingSet: Int)
   extends JsonReader[SparsePredictorProducer[K]]
      with VwMultilabelModelJson
      with Namespaces
      with Logging {

  import VwMultilabelModelPluginJsonReader._

  override def read(json: JsValue): VwSparseMultilabelPredictorProducer[K] = {
    val ast = json.asJsObject(notObjErr(json)).convertTo[VwMultilabelAst]
    val (namespaces, defaultNs, missing) =
      allNamespaceIndices(featureNames, ast.namespaces.getOrElse(ListMap.empty))

    if (missing.nonEmpty)
      info(s"features in namespaces not found in featureNames: $missing")

    val namespaceNames: Set[String] = namespaces.map(_._1)(breakOut)
    val labelAndDummyLabelNss = determineLabelNamespaces(namespaceNames)

    labelAndDummyLabelNss match {
      case Some(LabelNamespaces(labelNs, _)) =>
        VwSparseMultilabelPredictorProducer[K](ast.modelSource, defaultNs, namespaces, labelNs, numLabelsInTrainingSet)
      case _ =>
        throw new DeserializationException(
          "Could not determine label namespace.  Found namespaces: " +
            namespaceNames.mkString(", ")
        )
    }
  }
}

object VwMultilabelModelPluginJsonReader extends Logging {
  private val JsonErrStrLength = 100

  private[multilabel] def notObjErr(json: JsValue): String = {
    val str = json.prettyPrint
    val substr = str.substring(0, JsonErrStrLength)
    s"JSON object expected.  Found " + substr + (if (str.length != substr.length) " ..." else "")
  }
}