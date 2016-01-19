package com.eharmony.aloha.dataset

/**
  * A mixin that gives a standardized name to [[RowCreatorProducer]] instances.
  */
trait RowCreatorProducerName {
  def name: String = {
    val c = getClass
    (Option(c.getEnclosingClass) map { o => s"${o.getSimpleName}." } getOrElse "" ) + c.getSimpleName
  }
}
