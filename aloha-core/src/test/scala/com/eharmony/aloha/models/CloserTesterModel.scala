package com.eharmony.aloha.models

import java.util.concurrent.atomic.AtomicBoolean

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.ex.SchrodingerException
import com.eharmony.aloha.factory.{ModelSubmodelParsingPlugin, ModelParser, ParserProviderCompanion, SubmodelFactory}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import spray.json.{DeserializationException, JsValue, JsonFormat, JsonReader}
import spray.json.DefaultJsonProtocol.{BooleanJsonFormat, optionFormat}
import com.eharmony.aloha.factory.jsext.JsValueExtensions


case class CloserTesterModel[U, N, +B <: U](modelId: ModelIdentity, auditor: Auditor[U, N, B], shouldThrowOnClose: Boolean = false)
   extends Model[Any, B]
      with Submodel[N, Any, B] {

  private[this] val closed = new AtomicBoolean(false)
  def isClosed: Boolean = closed.get()
  override def apply(a: Any): B = throw new UnsupportedOperationException
  override def subvalue(a: Any): Subvalue[B, N] = throw new UnsupportedOperationException
  override def close(): Unit = {
    closed.set(true)
    if (shouldThrowOnClose)
      throw new SchrodingerException
  }
}

object CloserTesterModel extends ParserProviderCompanion {
  object Parser extends ModelSubmodelParsingPlugin {
    val modelType = "CloserTester"
    private val shouldThrowField = "shouldThrow"

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[CloserTesterModel[U, N, B]]] = {

      Some(new JsonReader[CloserTesterModel[U, N, B]] {
        override def read(json: JsValue): CloserTesterModel[U, N, B] = {
          val model = for {
            jsV <- json(shouldThrowField)
            mId <- getModelId(json)
            v = jsV.convertTo[Option[Boolean]]
            m = new CloserTesterModel(mId, auditor, v getOrElse false)
          } yield m

          model getOrElse { throw new DeserializationException("") }
        }
      })
    }
  }

  def parser: ModelParser = Parser
}