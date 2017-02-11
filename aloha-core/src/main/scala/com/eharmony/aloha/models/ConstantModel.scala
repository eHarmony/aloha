package com.eharmony.aloha.models


import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.factory.jsext.JsValueExtensions
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import spray.json.DefaultJsonProtocol.optionFormat
import spray.json.{DeserializationException, JsValue, JsonFormat, JsonReader}

case class ConstantModel[U, N, +B <: U](
    constant: Option[N],
    modelId: ModelIdentity,
    auditor: Auditor[U, N, B]
) extends SubmodelBase[U, N, Any, B] {
  def subvalue(a: Any): Subvalue[B, N] =
    constant.map(n => success(n))
            .getOrElse(failure(Seq("No constant supplied"), Set.empty))
}

object ConstantModel extends ParserProviderCompanion {
  object Parser extends ModelSubmodelParsingPlugin {
    val modelType = "Constant"
    private val valueField = "value"

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[ConstantModel[U, N, B]]] = {

      Some(new JsonReader[ConstantModel[U, N, B]] {
        override def read(json: JsValue): ConstantModel[U, N, B] = {
          val model = for {
            jsV <- json(valueField)
            mId <- getModelId(json)
            v = jsV.convertTo[Option[N]]
            m = new ConstantModel(v, mId, auditor)
          } yield m

          model getOrElse { throw new DeserializationException("") }
        }
      })
    }
  }

  def parser: ModelParser = Parser
}
