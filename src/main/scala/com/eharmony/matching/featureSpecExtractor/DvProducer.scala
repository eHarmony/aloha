package com.eharmony.matching.featureSpecExtractor

import com.eharmony.matching.aloha.AlohaException
import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.func.{GenAggFunc, GenFunc0}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
 * Helper to provide a way to construct dependent variables.
 */
trait DvProducer { self: CompilerFailureMessages =>

//    protected[this] def emptyDv[A] = GenFunc0("\"\"", (_: A) => "")

    protected[this] def getDv[A, B: RefInfo](semantics: CompiledSemantics[A], dvName: String, spec: Option[String], defVal: Option[B]): Try[GenAggFunc[A, B]] =
        spec map { s =>
            semantics.createFunction[B](s, defVal) match {
                case Left(msgs) => Failure(failure(dvName, msgs))
                case Right(f)   => Success(f)
            }
        } getOrElse { Failure(new AlohaException(s"dependent variable $dvName is not defined.")) }

    /**
     * Use this function to adapt the semantics to use implicits that convert values to strings.
     * @param semantics
     * @param imports
     * @tparam A
     * @return
     */
    protected[this] def addStringImplicitsToSemantics[A](semantics: CompiledSemantics[A], imports: Seq[String]): CompiledSemantics[A] =
        semantics.copy(imports = imports :+ "com.eharmony.matching.featureSpecExtractor.implicits._")
}
