package com.eharmony.aloha.interop

import java.{lang => jl}

import com.fasterxml.classmate.{TypeResolver, ResolvedType}

import com.eharmony.aloha.factory.JavaJsonFormats.JavaLongJsonFormat
import com.eharmony.aloha.reflect.{ReflectionConversions, RefInfo}
import ReflectionConversions.Implicits.ResolvedTypeToScalaReflection
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.JavaLongScoreConverter
import com.eharmony.aloha.reflect.RefInfo

class LongFactoryInfo[A](rt: ResolvedType) extends FactoryInfo[A, jl.Long] {

    /** This should only be used for non-parametrized types.  Behavior for parametrized types is undefined.
      * @param c
      * @return
      */
    def this(c: Class[A]) = this(new TypeResolver().resolve(c))
    val inRefInfo = rt.toRefInfo[A]
    val outRefInfo = RefInfo[jl.Long]
    val jsonReader = JavaLongJsonFormat
    val scoreConverter = JavaLongScoreConverter
}

