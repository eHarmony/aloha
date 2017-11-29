package com.eharmony.aloha.semantics.compiled

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.{AnyNameIdentitySemanticsPlugin, AnyNameMissingValueSemanticsPlugin}

/**
  * Created by ryan.deak on 9/26/17.
  */
object CompiledSemanticsInstances {

  /**
    * Creates a [[CompiledSemantics]] with the [[AnyNameIdentitySemanticsPlugin]] and with
    * imports `List("com.eharmony.aloha.feature.BasicFunctions._")`.
    * A class cache directory will be used.
    * @tparam A the domain of the functions created by this semantics.
    * @return
    */
  private[aloha] def anyNameIdentitySemantics[A: RefInfo]: CompiledSemantics[A] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    new CompiledSemantics(
      new TwitterEvalCompiler(classCacheDir = FileLocations.testGeneratedClasses),
      new AnyNameIdentitySemanticsPlugin[A],
      List("com.eharmony.aloha.feature.BasicFunctions._")
    )
  }

  /**
    * Creates a [[CompiledSemantics]] with the [[AnyNameMissingValueSemanticsPlugin]] and
    * the supplied `imports`.  A class cache directory will be used.
    * @param imports the imports to use.  The default is
    *                `List("com.eharmony.aloha.feature.BasicFunctions._")`.
    * @tparam A the domain of the functions created by this semantics.
    * @return
    */
  private[aloha] def anyNameMissingValueSemantics[A: RefInfo](
      imports: Seq[String] = List("com.eharmony.aloha.feature.BasicFunctions._")
  )(implicit ev: A <:< Option[_]): CompiledSemantics[A] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    new CompiledSemantics(
      new TwitterEvalCompiler(classCacheDir = FileLocations.testGeneratedClasses),
      new AnyNameMissingValueSemanticsPlugin[A],
      imports
    )
  }
}
