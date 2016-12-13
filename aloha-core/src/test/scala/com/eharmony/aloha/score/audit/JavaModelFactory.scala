package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.score.audit.JavaModelFactory.JavaModelFactoryException

/**
  * A facade around [[ModelFactory]] that provides more natural model creation from Java.
  * @param modelFactory
  */
case class JavaModelFactory(modelFactory: ModelFactory) {

  @throws(classOf[JavaModelFactoryException])
  def createConstantModel[A, N, B, MA <: MorphableAuditor[ModelIdentity, N, B, MA]](
      sem: Semantics[A],
      auditor: MorphableAuditor[ModelIdentity, N, B, MA],
      mId: ModelIdentity,
      constant: N): Model[A, B] = {
    modelFactory.createConstantModel(sem, auditor, mId, constant).fold(
      msg => throw new JavaModelFactoryException(msg),
      identity
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