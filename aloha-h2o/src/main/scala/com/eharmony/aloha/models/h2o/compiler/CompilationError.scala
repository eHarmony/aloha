package com.eharmony.aloha.models.h2o.compiler

import javax.tools.{JavaFileObject, Diagnostic}


/**
 * Created by deak on 9/29/15.
 */
case class CompilationError(diagnostics: Iterable[Diagnostic[_ <: JavaFileObject]]) extends RuntimeException
