package com.eharmony.aloha.factory

import com.eharmony.aloha.models.Model
import scala.beans.BeanProperty

case class ModelInfo[+M <: Model[_, _]](@BeanProperty model: M, fields: Seq[String])
