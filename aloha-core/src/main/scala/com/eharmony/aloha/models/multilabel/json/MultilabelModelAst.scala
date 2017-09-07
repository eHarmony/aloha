package com.eharmony.aloha.models.multilabel.json

import com.eharmony.aloha.models.reg.json.Spec

import scala.collection.immutable.ListMap

/**
  * Created by ryan.deak on 9/6/17.
  */
case class MultilabelModelAst(features: ListMap[String, Spec])

