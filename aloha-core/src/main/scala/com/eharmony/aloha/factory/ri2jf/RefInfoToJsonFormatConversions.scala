package com.eharmony.aloha.factory.ri2jf

import com.eharmony.aloha.reflect.RefInfo
import spray.json.JsonFormat

/**
  * Created by ryan on 1/25/17.
  */
trait RefInfoToJsonFormatConversions {
  def apply[A](implicit conv: RefInfoToJsonFormat, r: RefInfo[A], jf: JsonFormatCaster[A]): Option[JsonFormat[A]]
}
