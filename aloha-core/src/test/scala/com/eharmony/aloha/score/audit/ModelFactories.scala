package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId

object ModelFactories {

  /**
    * The canonical way to create a [[ModelFactory]].  While this may not be an idiomatic Scala API,
    * it is done so the creation of factories can be unified in Java and Scala.
    * @param aud a [[MorphableAuditor]] that is used to audit model output values '''and''' to create
    *            other auditors for submodels.
    * @tparam B The "''natural output type''" of the top-level model.  For instance, a regression model
    *           have a real-valued ''natural output type'', hence this would most likely be a `Float`
    *           or `Double`.
    * @tparam Y The auditor output type.  This is the same as the model output type.
    * @tparam MA The implementation of the [[MorphableAuditor]].
    * @return a [[ModelFactory]] used to create model instances.
    */
  def create[B, Y, MA <: MorphableAuditor[ModelId, B, Y, MA]](aud: MorphableAuditor[ModelId, B, Y, MA]): ModelFactory[B, Y, MA] =
    FactoryImpl[B, Y, MA](aud)

  private[this] case class FactoryImpl[B, Y, MA <: MorphableAuditor[ModelId, B, Y, MA]](aud: MorphableAuditor[ModelId, B, Y, MA]) extends ModelFactory[B, Y, MA] {
    def createConstantModel[A](sem: Semantics[A], mId: ModelId, b: B): Either[String, Model[A, Y]] =
      Right(ConstantModel(mId, b, aud))
  }
}
