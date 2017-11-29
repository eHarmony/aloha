package com.eharmony.aloha.semantics.compiled.plugin

import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.compiled.{CompiledSemanticsPlugin, OptionalAccessorCode}

/**
  * A semantics whose variables (''with any name'') always return missing data.
  * @param refInfoA reflection information about `A`
  * @param evOpt evidence that `A` is an `Option` of some kind.
  * @tparam A the input type to a model (an `Option` of some kind).
  * @author deaktator
  * @since 11/28/2017
  */
private[aloha] class AnyNameMissingValueSemanticsPlugin[A](implicit
    val refInfoA: RefInfo[A],
    evOpt: A <:< Option[_])
  extends CompiledSemanticsPlugin[A] {

  /**
    * Produces code for a function that takes an `Option[A]` and returns an `Option.empty[A]`.
    * @param spec an '''ignored''' string-based specification of a function.
    * @return
    */
  override def accessorFunctionCode(spec: String): Right[Nothing, OptionalAccessorCode] =
    Right(OptionalAccessorCode(Seq(s"(o: ${RefInfoOps.toString[A]}) => o.filterNot(_ => true)")))
}
