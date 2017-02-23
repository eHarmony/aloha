package com.eharmony.aloha.factory.ri2jf

import com.eharmony.aloha.reflect.RefInfo
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

/**
  * Created by ryan on 1/25/17.
  */
class BasicTypes extends RefInfoToJsonFormatConversions {
  def apply[A](implicit conv: RefInfoToJsonFormat, r: RefInfo[A], jf: JsonFormatCaster[A]): Option[JsonFormat[A]] = {
    if (r == RefInfo[Boolean])         jf[Boolean]
    else if (r == RefInfo[Byte])       jf[Byte]
    else if (r == RefInfo[Short])      jf[Short]
    else if (r == RefInfo[Int])        jf[Int]
    else if (r == RefInfo[Long])       jf[Long]
    else if (r == RefInfo[Float])      jf[Float]
    else if (r == RefInfo[Double])     jf[Double]
    else if (r == RefInfo[BigDecimal]) jf[BigDecimal]
    else if (r == RefInfo[BigInt])     jf[BigInt]
    else if (r == RefInfo[Unit])       jf[Unit]
    else if (r == RefInfo[Char])       jf[Char]
    else if (r == RefInfo[Symbol])     jf[Symbol]
    else if (r == RefInfo[String])     jf[String]
    else None
  }
}
