package com.eharmony.aloha.models

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.factory.jsext.JsValueExtensions
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import spray.json.{DeserializationException, JsValue, JsonFormat, JsonReader}


case class ErrorModel[U, +B <: U](modelId: ModelIdentity, errors: Seq[String], auditor: Auditor[U, Nothing, B]) extends SubmodelBase[U, Nothing, Any, B] {
  def subvalue(a: Any): Subvalue[B, Nothing] = failure(errors, Set.empty)
  override def toString(): String = "ErrorModel(" + modelId + "," + errors + ")"
}

object ErrorModel extends ParserProviderCompanion {
  object Parser extends ModelSubmodelParsingPlugin {
    val modelType = "Error"
    private[this] val errorField = "errors"

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[ErrorModel[U, B]]] = {

      Some(new JsonReader[ErrorModel[U, B]] {
        def read(json: JsValue): ErrorModel[U, B] = {
          val model = for {
            errors <- json.sa(errorField) orElse { Option(Seq("Error with unspecified reason.")) }
            mId <- getModelId(json)
          } yield ErrorModel(mId, errors, auditor)

          model getOrElse { throw new DeserializationException("") }
        }
      })
    }
  }

  def parser: ModelParser = Parser
}
