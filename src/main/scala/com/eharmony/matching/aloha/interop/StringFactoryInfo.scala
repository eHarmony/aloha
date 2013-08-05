package com.eharmony.matching.aloha.interop

import java.{lang => jl}

import com.fasterxml.classmate.{TypeResolver, ResolvedType}

import spray.json.DefaultJsonProtocol.StringJsonFormat
import com.eharmony.matching.aloha.reflect.{ReflectionConversions, RefInfo}
import ReflectionConversions.Implicits.ResolvedTypeToScalaReflection
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.StringScoreConverter
import com.eharmony.matching.aloha.reflect.RefInfo

class StringFactoryInfo[A](rt: ResolvedType) extends FactoryInfo[A, String] {

    /** This should only be used for non-parametrized types.  Behavior for parametrized types is undefined.
      * @param c
      * @return
      */
    def this(c: Class[A]) = this(new TypeResolver().resolve(c))
    val inRefInfo = rt.toRefInfo[A]
    val outRefInfo = RefInfo[String]
    val jsonReader = StringJsonFormat
    val scoreConverter = StringScoreConverter
}

