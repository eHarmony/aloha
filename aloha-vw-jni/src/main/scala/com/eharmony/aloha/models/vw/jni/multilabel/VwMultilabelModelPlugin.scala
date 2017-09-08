package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.Closeable

import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.io.sources.ModelSource
import com.eharmony.aloha.models.multilabel._
import com.eharmony.aloha.models.multilabel.json.MultilabelModelAst
import com.eharmony.aloha.reflect.RefInfo
import spray.json.{JsValue, JsonFormat, JsonReader}
import spray.json.DefaultJsonProtocol._

import scala.collection.immutable.ListMap
import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 9/5/17.
  */
case class VwMultilabelModelPlugin[K](
    modelSource: ModelSource,
    params: String,
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])])
extends SparsePredictorProducer[K] {

  import VwMultilabelModelPlugin._

  override def apply(): VwSparsePredictorProducer[K] =
    VwSparsePredictorProducer[K](modelSource, params, defaultNs, namespaces)
}

object VwMultilabelModelPlugin extends MultilabelPluginProviderCompanion {

  private val JsonErrStrLength = 100

  def multilabelPlugin = Plugin
  object Plugin extends MultilabelModelParserPlugin {
    override def name: String = "vw"
    override def parser[K](ast: MultilabelModelAst)
                          (implicit ri: RefInfo[K], jf: JsonFormat[K]): VwMultilabelModelPluginJsonReader[K] = {
      VwMultilabelModelPluginJsonReader[K](ast.features.keys.toVector)
    }
  }


  private[multilabel] case class VwMultilabelAst(
      modelSource: ModelSource,
      params: Option[Either[Seq[String], String]] = Option(Right("")),
      namespaces: Option[ListMap[String, Seq[String]]] = Some(ListMap.empty)
  )

  private[this] implicit val vwMultilabelAstFormat = jsonFormat3(VwMultilabelAst)

  case class VwSparsePredictorProducer[K](
      modelSource: ModelSource,
      params: String,
      defaultNs: List[Int],
      namespaces: List[(String, List[Int])])
  extends SparseMultiLabelPredictor[K]
     with Closeable {

    // TODO: Create the model.

    override def apply(
        features: IndexedSeq[Sparse],
        labels: sci.IndexedSeq[K],
        indices: sci.IndexedSeq[Int],
        labelDependentFeatures: sci.IndexedSeq[IndexedSeq[Sparse]]
    ): Map[K, Double] = {

      // Create VW input.

      ???
    }

    override def close(): Unit = {
      // TODO: Add actual
    }
  }

  case class VwMultilabelModelPluginJsonReader[K](featureNames: Vector[String]) extends JsonReader[VwMultilabelModelPlugin[K]] {

    private[multilabel] def namespaceMapping(featureNames: Vector[String], nsMap: ListMap[String, Seq[String]]): (List[Int], List[(String, List[Int])]) = {

      ???
    }

    override def read(json: JsValue): VwMultilabelModelPlugin[K] = {
      val ast = json.asJsObject(notObjErr(json)).convertTo[VwMultilabelAst]

      val params = ast.params.map {
        case Left(paramList)  => paramList.mkString(" ")
        case Right(paramsStr) => paramsStr
      }

      params.map { ps =>
        val (defaultNs, namespaces) = namespaceMapping(featureNames, ast.namespaces.getOrElse(ListMap.empty))
        VwMultilabelModelPlugin[K](ast.modelSource, ps, defaultNs, namespaces)
      } getOrElse {
        throw new Exception("no VW params provided")
      }
    }

    protected[multilabel] def notObjErr(json: JsValue): String = {
      val str = json.prettyPrint
      val substr = str.substring(0, JsonErrStrLength)
      s"JSON object expected.  Found " + substr + (if (str.length != substr.length) " ..." else "")
    }
  }
}
