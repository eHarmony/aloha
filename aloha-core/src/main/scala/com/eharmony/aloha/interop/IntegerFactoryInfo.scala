package com.eharmony.aloha.interop

import java.{lang => jl}

import com.fasterxml.classmate.{TypeResolver, ResolvedType}

import com.eharmony.aloha.factory.JavaJsonFormats.JavaIntJsonFormat
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.JavaIntScoreConverter
import com.eharmony.aloha.reflect.{ReflectionConversions, RefInfo}
import ReflectionConversions.Implicits.ResolvedTypeToScalaReflection
import com.eharmony.aloha.reflect.RefInfo

class IntegerFactoryInfo[A](rt: ResolvedType) extends FactoryInfo[A, jl.Integer] {

    /** This should only be used for non-parametrized types.  Behavior for parametrized types is undefined.
      * @param c
      * @return
      */
    def this(c: Class[A]) = this(new TypeResolver().resolve(c))
    val inRefInfo = rt.toRefInfo[A]
    val outRefInfo = RefInfo[jl.Integer]
    val jsonReader = JavaIntJsonFormat
    val scoreConverter = JavaIntScoreConverter
}

