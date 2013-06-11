package com.eharmony.matching.aloha.factory

import com.eharmony.matching.aloha.models.Model
import scala.beans.BeanProperty

case class ModelInfo[+M <: Model[_, _]](@BeanProperty model: M, fields: Seq[String])
