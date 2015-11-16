package com.eharmony.aloha.models

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

import com.eharmony.aloha.ex.SchrodingerException
import com.eharmony.aloha.id.{ModelId, ModelIdentity}

class CloserTesterModel[+A](shouldThrowOnClose: Boolean = false) extends BaseModel[Any, A] with Closeable {
  private[this] val closed = new AtomicBoolean(false)
  def isClosed = closed.get()
  override val modelId: ModelIdentity = ModelId.empty
  override private[aloha] def getScore(a: Any)(implicit audit: Boolean) = failure(Seq("not implemented intentionally"))
  override def close() = {
    closed.set(true)
    if (shouldThrowOnClose)
      throw new SchrodingerException
  }
}
