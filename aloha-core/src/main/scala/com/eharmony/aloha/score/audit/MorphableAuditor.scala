package com.eharmony.aloha.score.audit

import com.eharmony.aloha.reflect.RefInfo

/**
  * A [[MorphableAuditor]] extends [[Auditor]]'s auditing capabilities by including
  * information about its own implementation type.  This allows the [[MorphableAuditor]]
  * to produce new instances of itself but with a different type parameters.  This is
  * useful when recursive auditing of different types at different levels of an audit
  * trail are necessary.  To use this, you can do something like:
  *
  * {{{
  * class Sem[In, Aud <: MorphableAuditor[Long, Nothing, Any, Aud]](aud: Aud) {
  *   def audit[V: RefInfo](v: V): Option[Aud#Audited[V]] = aud.auditor[V].map(a => a.success(1L, v))
  * }
  * }}}
  *
  * The `Nothing` and `Any` are important because the `A` and `B` are contravariant and
  * covariant, respectively.
  *
  *
  * '''Note''': The decision to include the Impl type parameter wasn't made lightly.
  * It is provided so that the return type of `MorphableAuditor.auditor` are
  * [[https://ktoso.github.io/scala-types-of-types/#type-projection type projections]]
  * rather than
  * [[https://ktoso.github.io/scala-types-of-types/#path-dependent-type path-dependent types]].
  * This turns out to be extremely important.  If we didn't use type projections and
  * omitted this type parameter, the output type would be a path-dependent type, meaning
  * it would be dependent on the '''instance''' of the [[MorphableAuditor]].  Those who have
  * worked with Scala reflection `Universe`s will understand this.  A concrete example of
  * this can be seen here:
  *
  * {{{
  * import com.eharmony.aloha.score.audit.NoAudit
  *
  * val na = NoAudit[Long, Int]()
  * val impl = na.auditor[Float].get
  * val auditedValue = impl.success(1L, 1.5f).get
  * }}}
  *
  * This code will work regardless of whether we include the `Impl` type parameter in
  * [[MorphableAuditor]].  The difference is in the resulting type of `auditedValue`.  If
  * `Impl` is omitted, we would get a type of `na.Audited[Float]`.  If subsequently,
  * we tried to call the following code, we'd get a compiler error:
  *
  * {{{
  * def floatIdentity(f: Float) = f
  * floatIdentity(auditedValue)
  * }}}
  *
  * If however, we use type projections, we don't get this compiler because
  * `auditedValue` is related to the type of `na` and not tied to the instance `na`
  * itself.
  *
  * Created by deaktator on 12/9/16.
  *
  * @tparam K The key type
  * @tparam A The raw values to audit.  This is the ''natural'' type of a model.
  *             For instance, in a regression model, the `A` type would be
  *             something `float` or `double` because regression models produce
  *             real-valued output.
  * @tparam B The final output type.  This is the same as the model output type.
  * @tparam Impl The concrete implementation.
  */
// TODO: Specialization
trait MorphableAuditor[K, A, +B, Impl <: MorphableAuditor[K, A, B, Impl]]
  extends Auditor[K, A, B] {

  /**
    * Attempt to create a new typed auditor with a new `A` type equal to `C`.
    * @tparam C The new `A` type in the Auditor
    * @return an [[Auditor]] capable of auditing values of type `C`.  If the
    *         auditor could be produced, it will be wrapped in a `Some`.  If no
    *         such auditor could be produced (i.e., the auditor doesn't support
    *         values of type `C`), a `None` will be returned.
    */
  def auditor[C: RefInfo]: Option[Auditor[K, C, Impl#AuditOutput[C]]]
}
