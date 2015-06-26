package com.eharmony.aloha.id

import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol.{jsonFormat2, LongJsonFormat, StringJsonFormat}
import scala.beans.BeanProperty

case class ModelId(@BeanProperty id: Long = 0, @BeanProperty name: String = "") extends ModelIdentity

object ModelId {
    val empty = ModelId()

    /**
     * Makes it easy to ask for a RootJsonFormat[ModelId] because this will automatically be found
     * in the implicit scope.
     */
    implicit val jsonFormat: RootJsonFormat[ModelId] = jsonFormat2(ModelId.apply)
}
