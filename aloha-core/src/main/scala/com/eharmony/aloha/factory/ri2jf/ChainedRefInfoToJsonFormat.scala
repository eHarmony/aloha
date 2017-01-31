package com.eharmony.aloha.factory.ri2jf

import com.eharmony.aloha.reflect.RefInfo
import spray.json.JsonFormat

/**
  * Created by ryan on 1/25/17.
  */
trait ChainedRefInfoToJsonFormat extends RefInfoToJsonFormat {
  def conversionTypes: Stream[RefInfoToJsonFormatConversions]
  def apply[A](implicit r: RefInfo[A]): Option[JsonFormat[A]] = {
    val c = new JsonFormatCaster[A]

    conversionTypes.
      map { f => f(this, r, c) }.
      collectFirst { case Some(jf) => jf }
  }
}