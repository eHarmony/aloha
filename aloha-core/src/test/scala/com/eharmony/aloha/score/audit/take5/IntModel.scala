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
    auditor: Auditor[U, _ >: Int <: Int, B]
) extends AuditedModel[U, Int, Any, B] {
  override def apply(v1: Any): B = auditor.success(modelId, value, Set.empty, Nil, None)
}

object IntModel {

  /**
    * A convenience method that is purely syntactic to enable proper type checking in Java.
    * The problem with a model having a natural type that is a subtype of `AnyVal` is that
    * these types resolve to `Object` when the byte code is emitted.  To get around this,
    * we provide a Java-oriented factory method where the type parameter `N` is a supertype
    * and subtype of `Float`, and becomes an unbound type parameter in the emitted code.
    * Ultimately, this resolves to the following Java code:
    *
    * `
    * public static <U, N, B extends U> FloatModel<U, B>
    * createFromJava(ModelIdentity modelId, float value, Auditor<U, N, B> auditor)
    * `
    *
    * If instead of including this convenience method, we chose to change the definition of
    * the `auditor` parameter in the [[IntModel]] constructor to:
    *
    * {{{
    * auditor: Auditor[U, _ >: Float, B]
    * }}}
    *
    * and attempted to instantiate the model via:
    *
    * `
    * final FloatModel<Option<?>, Option<Float>> model =
    *   new FloatModel<>(modelId, expected, auditor);
    * `
    *
    * We'd see the following error:
    *
    * `
    * Error:(58, 60) java: cannot infer type arguments for com.eharmony.aloha.score.audit.take5.FloatModel<>
    *   reason: cannot infer type-variable(s) U,B
    *     (argument mismatch; com.eharmony.aloha.score.audit.take5.OptionAuditor<java.lang.Float>
    *       cannot be converted to com.eharmony.aloha.score.audit.take5.Auditor<U,? super java.lang.Object,B>)
    * `
    *
    * @param modelId a model identifier
    * @param value a value returned by the model.
    * @param auditor an auditor capable of auditing a `Float` and turning it into a `B`.
    * @tparam U The upper bound pof the audited type hierarchy.
    * @tparam N a super type of the model's natural type: `Float`.
    * @tparam B The output type.
    * @return
    */
  def createFromJava[U, N >: Int <: Int, B <: U](modelId: ModelIdentity, value: Int, auditor: Auditor[U, N, B]): IntModel[U, B] =
    IntModel(modelId, value, auditor)
}
