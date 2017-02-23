package com.eharmony.aloha.factory.ri2jf

import spray.json.JsonFormat

/**
  * Created by ryan on 1/25/17.
  */
class JsonFormatCaster[A] {
  def apply[R](implicit r: JsonFormat[R]): Some[JsonFormat[A]] =
    Some(r.asInstanceOf[JsonFormat[A]])
}