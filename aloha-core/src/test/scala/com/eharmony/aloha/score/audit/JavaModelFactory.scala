package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.score.audit.JavaModelFactory.JavaModelFactoryException

case class JavaModelFactory[B, Y, MA <: MorphableAuditor[ModelId, B, Y, MA]](
    modelFactory: ModelFactory[B, Y, MA]) {

  @throws(classOf[JavaModelFactoryException])
  def createConstantModel[A](sem: Semantics[A], mId: ModelId, b: B): Model[A, Y] = {
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