package com.eharmony.aloha.models.vw.jni

import scala.collection.immutable.ListMap

/**
  * Created by ryan.deak on 9/8/17.
  */
trait Namespaces {

  def namespaceIndicesAndMissing(
      indices: Map[String, Int],
      namespaces: ListMap[String, Seq[String]]
  ): (List[(String, List[Int])], List[(String, List[String])]) = {
    val nss = namespaces.map { case (ns, fs) =>
      val indMissing = fs.map { f =>
        val oInd = indices.get(f)
        if (oInd.isEmpty)
          (oInd, Option(f))
        else (oInd, None)
      }

      val (oInd, oMissing) = indMissing.unzip
      (ns, oInd.flatten.toList, oMissing.flatten.toList)
    }

    val nsInd = nss.collect { case (ns, ind, _) if ind.nonEmpty => (ns, ind) }.toList
    val nsMissing = nss.collect { case (ns, _, m) if m.nonEmpty => (ns, m) }.toList
    (nsInd, nsMissing)
  }

  def defaultNamespaceIndices(
      indices: Map[String, Int],
      namespaces: ListMap[String, Seq[String]]
  ): List[Int] = {
    val featuresInNss = namespaces.foldLeft(Set.empty[String]){ case (s, (_, fs)) => s ++ fs }
    val unusedFeatures = indices.keySet -- featuresInNss
    unusedFeatures.flatMap(indices.get).toList
  }

  def allNamespaceIndices(
      featureNames: Seq[String],
      namespaces: ListMap[String, Seq[String]]
  ): (List[(String, List[Int])], List[Int], List[(String, List[String])]) = {
    val indices = featureNames.zipWithIndex.toMap
    val (nsi, missing) = namespaceIndicesAndMissing(indices, namespaces)
    val defaultNsInd = defaultNamespaceIndices(indices, namespaces)

    (nsi, defaultNsInd, missing)
  }
}
