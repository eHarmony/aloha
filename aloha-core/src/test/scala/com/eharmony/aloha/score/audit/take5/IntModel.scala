package com.eharmony.aloha.score.audit.take5

import com.eharmony.aloha.id.ModelIdentity

/**
  * Created by ryan on 1/12/17.
  *
  * @param modelId
  * @param value
  * @param auditor
  * @tparam U
  * @tparam B
  */
case class IntModel[U, +B <: U](
    modelId: ModelIdentity,
    value: Int,
    auditor: Auditor[U, Int, B]
) extends Model[Any, B] {
  override def apply(v1: Any): B = auditor.success(modelId, value, Set.empty, Nil, None)
}

object IntModel {

  /**
    * A convenience method that is purely syntactic to enable proper type checking in Java.
    * The problem with a model having a natural type that is a subtype of `AnyVal` is that
    * these types resolve to `Object` when the byte code is emitted.  To get around this,
    * we provide a Java-oriented factory method where the type parameter `N` is a supertype
    * and subtype of `Int`, and becomes an unbound type parameter in the emitted code.
    * Ultimately, this resolves to the following Java code:
    *
    * `
    * public static <U, N, B extends U> IntModel<U, B>
    *   createFromJava(ModelIdentity modelId, int value, Auditor<U, N, B> auditor)
    * `
    *
    * @param modelId a model identifier
    * @param value a value returned by the model.
    * @param auditor an auditor capable of auditing a `Int` and turning it into a `B`.
    * @tparam U The upper bound pof the audited type hierarchy.
    * @tparam N a super type of the model's natural type: `Int`.
    * @tparam B The output type.
    * @return
    */
  def createFromJava[U, N >: Int <: Int, B <: U](modelId: ModelIdentity, value: Int, auditor: Auditor[U, N, B]): IntModel[U, B] =
    IntModel(modelId, value, auditor)
}
