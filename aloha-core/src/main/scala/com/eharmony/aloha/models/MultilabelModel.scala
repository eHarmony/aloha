package com.eharmony.aloha.models

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.Semantics
import spray.json.{JsonFormat, JsonReader}

sealed trait LabelExtraction[-A, K]
final case class KnownLabelExtraction[K](labels: Seq[K]) extends LabelExtraction[Any, K]
final case class PerExampleLabelExtraction[A, K](extractor: A => Seq[K]) extends LabelExtraction[A, K]

/**
  * Created by ryan.deak on 8/29/17.
  *
  * @param modelId
  * @param labelExtraction
  * @param labelDependentFeatures
  * @param featureExtraction
  * @param predictorProducer
  * @param auditor
  * @tparam U upper bound on model output type `B`
  * @tparam F type of features produced
  * @tparam K type of label or class
  * @tparam A input type of the model
  * @tparam B output type of the model.
  */
case class MultilabelModel[U, F, K, -A, +B <: U](
    modelId: ModelIdentity,
    labelExtraction: LabelExtraction[A, K],
    labelDependentFeatures: Seq[K => F],
    featureExtraction: Seq[A => F],
    predictorProducer: () => (Seq[F], Seq[K]) => Map[K, Double],
    auditor: Auditor[U, Map[K, Double], B]
)
extends SubmodelBase[U, Map[K, Double], A, B] {

  @transient private[this] lazy val predictor = predictorProducer()

  {
    // Force predictor eagerly
    predictor
  }

  private val globalLdf = labelExtraction match {
    case KnownLabelExtraction(labels) => Option(applyLdf(labels))
    case _ => None
  }

  private[this] def applyLdf(labels: Seq[K]): Seq[Seq[F]] =
    labels.map(label => labelDependentFeatures.map(f => f(label)))

  override def subvalue(a: A): Subvalue[B, Map[K, Double]] = {
    val labels = labelExtraction match {
      case KnownLabelExtraction(ls) => ls
      case PerExampleLabelExtraction(extractor) => extractor(a)
    }

    // Get the label-dependent features.
    val ldf = globalLdf getOrElse applyLdf(labels)

    val features = featureExtraction.map(f => f(a))
    val natural = predictor(features, labels)
    val aud = auditor.success(modelId, natural)
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
