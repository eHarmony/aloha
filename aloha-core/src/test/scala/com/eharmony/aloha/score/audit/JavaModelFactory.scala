package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.score.audit.JavaModelFactory.JavaModelFactoryException

case class JavaModelFactory[N, B, MA <: MorphableAuditor[ModelId, N, B, MA]](
    modelFactory: ModelFactory[N, B, MA]) {

  @throws(classOf[JavaModelFactoryException])
  def createConstantModel[A](sem: Semantics[A], mId: ModelId, b: N): Model[A, B] = {
    // There shouldn't be any recoverable Exceptions thrown in the Scala factory methods.
    modelFactory.createConstantModel(sem, mId, b).fold(
      msg => throw new JavaModelFactoryException(msg),
      m => m
    )
  }
}

object JavaModelFactory {
  final class JavaModelFactoryException(message: String, cause: Throwable) extends Exception(message, cause) {
    def this() = this(null, null)
    def this(message: String) = this(message, null)
    def this(cause: Throwable) = this(null, cause)
  }
}