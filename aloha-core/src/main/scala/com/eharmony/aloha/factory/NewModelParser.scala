package com.eharmony.aloha.factory

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory.jsext.JsValueExtensions
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.models.{Model, Submodel}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, JsonFormat, JsonReader}
import spray.json.DefaultJsonProtocol.{LongJsonFormat, StringJsonFormat}

/**
  * Created by ryan on 1/26/17.
  */
sealed trait NewModelParser {

  val modelType: String

  private implicit val modelIdFormat = DefaultJsonProtocol.jsonFormat2(ModelId.apply)

  protected final def getModelId(json: JsValue): Option[ModelIdentity] =
    json(NewModelParser.modelIdField).collect{case o: JsObject => o.convertTo[ModelId]}
}

private object NewModelParser {
  val modelIdField = "modelId"
}

trait ModelParsingPlugin extends NewModelParser {
  def modelJsonReader[U, N, A, B <: U](factory: SubmodelFactory[U, A], semantics: Semantics[A], auditor: Auditor[U, N, B])
                                      (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[Model[A, B]]]
}

trait SubmodelParsingPlugin extends NewModelParser {
  def submodelJsonReader[U, N, A, B <: U](factory: SubmodelFactory[U, A], semantics: Semantics[A], auditor: Auditor[U, N, B])
                                         (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[Submodel[N, A, U]]]
}

trait ModelSubmodelParsingPlugin extends ModelParsingPlugin with SubmodelParsingPlugin {
  def commonJsonReader[U, N, A, B <: U](factory: SubmodelFactory[U, A], semantics: Semantics[A], auditor: Auditor[U, N, B])
                                       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[_ <: Model[A, B] with Submodel[_, A, B]]]

  final override def modelJsonReader[U, N, A, B <: U](factory: SubmodelFactory[U, A], semantics: Semantics[A], auditor: Auditor[U, N, B])
                                                     (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[Model[A, B]]] = {
    val reader = commonJsonReader(factory, semantics, auditor)
    reader.map(jr => jr.asInstanceOf[JsonReader[Model[A, B]]])
  }

  final def submodelJsonReader[U, N, A, B <: U](factory: SubmodelFactory[U, A], semantics: Semantics[A], auditor: Auditor[U, N, B])
                                               (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[Submodel[N, A, U]]] = {
    val reader = commonJsonReader(factory, semantics, auditor)
    reader.map(jr => jr.asInstanceOf[JsonReader[Submodel[N, A, U]]])
  }
}

