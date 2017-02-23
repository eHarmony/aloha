package com.eharmony.aloha.factory.ri2jf

import java.{lang => jl}

import com.eharmony.aloha.factory.JavaJsonFormats
import com.eharmony.aloha.reflect.RefInfo
import spray.json.JsonFormat

/**
  * Created by ryan on 2/9/17.
  */
class JavaTypes extends RefInfoToJsonFormatConversions {
  def apply[A](implicit conv: RefInfoToJsonFormat, r: RefInfo[A], jf: JsonFormatCaster[A]): Option[JsonFormat[A]] = {
    if (r == RefInfo[jl.Boolean])         jf(JavaJsonFormats.JavaBooleanJsonFormat)
    else if (r == RefInfo[jl.Byte])       jf(JavaJsonFormats.JavaByteJsonFormat)
    else if (r == RefInfo[jl.Short])      jf(JavaJsonFormats.JavaShortJsonFormat)
    else if (r == RefInfo[jl.Integer])    jf(JavaJsonFormats.JavaIntJsonFormat)
    else if (r == RefInfo[jl.Long])       jf(JavaJsonFormats.JavaLongJsonFormat)
    else if (r == RefInfo[jl.Float])      jf(JavaJsonFormats.JavaFloatJsonFormat)
    else if (r == RefInfo[jl.Double])     jf(JavaJsonFormats.JavaDoubleJsonFormat)
    else None
  }
}
