package com.eharmony.aloha.factory

import com.eharmony.aloha.models.Submodel
import com.eharmony.aloha.reflect.RefInfo
import spray.json.{JsValue, JsonFormat}

import scala.util.Try

/**
  * Created by ryan on 1/31/17.
  */
trait SubmodelFactory[U, A] {
  def submodel[N](json: JsValue)(implicit r: RefInfo[N]): Try[Submodel[N, A, U]]
  def jsonFormat[N: RefInfo]: Option[JsonFormat[N]]
}
