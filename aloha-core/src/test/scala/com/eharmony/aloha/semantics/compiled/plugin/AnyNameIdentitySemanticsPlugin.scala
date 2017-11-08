package com.eharmony.aloha.semantics.compiled.plugin

import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.semantics.compiled.{CompiledSemanticsPlugin, RequiredAccessorCode}

/**
  * This plugin binds ANY variable name to the input.  For example, if the domain of
  * the generated function is `Int` and the function specification is any of the following:
  *
    - `"${asdf} + 1"`
    - `"${jkl} + 1"`
    - `"${i_dont_care_the_name} + 1"`
    - ...
  *
  * the resulting function adds 1 to the input.
  *
  * Created by ryan.deak on 9/26/17.
  * @param refInfoA reflection information about the function domain.
  * @tparam A domain of the functions being generated from this plugin.
  */
private[aloha] class AnyNameIdentitySemanticsPlugin[A](implicit val refInfoA: RefInfo[A])
  extends CompiledSemanticsPlugin[A] {
  override def accessorFunctionCode(spec: String): Right[Nothing, RequiredAccessorCode] =
    Right(RequiredAccessorCode(Seq(s"identity[${RefInfoOps.toString[A]}]")))
}
