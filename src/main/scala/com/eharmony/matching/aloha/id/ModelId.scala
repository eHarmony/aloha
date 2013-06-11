package com.eharmony.matching.aloha.id

import scala.beans.BeanProperty

case class ModelId(@BeanProperty id: Long = 0, @BeanProperty name: String = "") extends ModelIdentity

object ModelId {
    val empty = ModelId()
}
