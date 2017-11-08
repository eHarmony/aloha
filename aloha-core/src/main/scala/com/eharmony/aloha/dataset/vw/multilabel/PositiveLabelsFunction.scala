package com.eharmony.aloha.dataset.vw.multilabel

import com.eharmony.aloha.AlohaException
import com.eharmony.aloha.dataset.DvProducer
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator.{determineLabelNamespaces, LabelNamespaces}
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc

import scala.collection.breakOut
import scala.util.{Failure, Success, Try}
import scala.collection.{immutable => sci}

/**
  * Created by ryan.deak on 11/6/17.
  * @param ev$1
  * @tparam A
  * @tparam K
  */
private[multilabel] abstract class PositiveLabelsFunction[A, K: RefInfo] { self: DvProducer =>

  private[multilabel] def positiveLabelsFn(
      semantics: CompiledSemantics[A],
      positiveLabels: String
  ): Try[GenAggFunc[A, sci.IndexedSeq[K]]] =
    getDv[A, sci.IndexedSeq[K]](
      semantics, "positiveLabels", Option(positiveLabels), Option(Vector.empty[K]))

  private[multilabel] def labelNamespaces(nss: List[(String, List[Int])]): Try[LabelNamespaces] = {
    val nsNames: Set[String] = nss.map(_._1)(breakOut)
    determineLabelNamespaces(nsNames) match {
      case Some(ns) => Success(ns)

      // If there are so many VW namespaces that all available Unicode characters are taken,
      // then a memory error will probably already have occurred.
      case None => Failure(new AlohaException(
        "Could not find any Unicode characters to as VW namespaces. Namespaces provided: " +
          nsNames.mkString(", ")
      ))
    }
  }
}
