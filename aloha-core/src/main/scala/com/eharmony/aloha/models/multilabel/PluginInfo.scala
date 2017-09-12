package com.eharmony.aloha.models.multilabel

import com.eharmony.aloha.models.reg.json.Spec

import scala.collection.immutable.ListMap

/**
  * Created by ryan.deak on 9/7/17.
  */
trait PluginInfo {
  def features: ListMap[String, Spec]
}
