package com.eharmony.aloha.interop

import java.{lang => jl}

import com.fasterxml.classmate.{TypeResolver, ResolvedType}

import com.eharmony.aloha.factory.JavaJsonFormats.JavaDoubleJsonFormat
import com.eharmony.aloha.reflect.{ReflectionConversions, RefInfo}
import ReflectionConversions.Implicits.ResolvedTypeToScalaReflection
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.JavaDoubleScoreConverter
import com.eharmony.aloha.reflect.RefInfo

class DoubleFactoryInfo[A](rt: ResolvedType) extends FactoryInfo[A, jl.Double] {

    /** This should only be used for non-parametrized types.  Behavior for parametrized types is undefined.
      * @param c
      * @return
      */
    def this(c: Class[A]) = this(new TypeResolver().resolve(c))
    val inRefInfo = rt.toRefInfo[A]
    val outRefInfo = RefInfo[jl.Double]
    val jsonReader = JavaDoubleJsonFormat
    val scoreConverter = JavaDoubleScoreConverter
}

