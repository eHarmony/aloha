package com.eharmony.aloha.models

import java.{util => ju}

import com.eharmony.aloha.id.ModelIdentity

import scala.collection.JavaConversions.seqAsJavaList

case class TopLevelModel[-A, +B](model: Model[A, B], fields: Seq[String]) extends Model[A, B] {
  def modelId: ModelIdentity = model.modelId
  def getFieldList: ju.List[String] = seqAsJavaList(fields)
  override def apply(a: A): B = model(a)
  override def close(): Unit = ()
}
