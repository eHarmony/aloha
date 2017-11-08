package com.eharmony.aloha.semantics.compiled

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.AnyNameIdentitySemanticsPlugin

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
}
