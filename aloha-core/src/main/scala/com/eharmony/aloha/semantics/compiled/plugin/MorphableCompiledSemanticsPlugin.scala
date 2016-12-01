package com.eharmony.aloha.semantics.compiled.plugin

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.CompiledSemanticsPlugin

/**
  * Provides a way to adapt a [[CompiledSemanticsPlugin]] to a new one with a
  * different type parameter.
  * @author deaktator
  */
trait MorphableCompiledSemanticsPlugin {

  /**
    * Attempt to create a new [[CompiledSemanticsPlugin]] with type parameter[B]
    * @param ri reflection information that may be necessary to determine whether to create
    *           the [[CompiledSemanticsPlugin]] that was requested.
    * @tparam B input type for the new [[CompiledSemanticsPlugin]] instance that might be created.
    *           A [[MorphableCompiledSemanticsPlugin]] instance may choose not allow morphing
    *           to all `B`. In that case, a `None` will be returned.
    * @return
    */
  def morph[B](implicit ri: RefInfo[B]): Option[CompiledSemanticsPlugin[B]]
}
