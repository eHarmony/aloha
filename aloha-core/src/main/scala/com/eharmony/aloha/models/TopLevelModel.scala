package com.eharmony.aloha.models

import collection.JavaConversions.seqAsJavaList

case class TopLevelModel[-A, +B](model: Model[A, B], fields: Seq[String]) extends BaseModel[A, B] {
    def getFieldList = seqAsJavaList(fields)
    val modelId = model.modelId
    private[aloha] def getScore(a: A)(implicit audit: Boolean) = model.getScore(a)
}
