package com.eharmony.aloha.models.vw.jni.multilabel

import com.eharmony.aloha.models.vw.jni.multilabel.VwSparseMultilabelPredictor.ExpectedLearner

/**
  * Created by ryan.deak on 10/5/17.
  */
sealed trait VwParamError {
  def originalParams: String
  def modifiedParams: Option[String]
  def errorMessage: String
}

final case class UnrecoverableParams(
    originalParams: String,
    unrecoverable: Set[String]
) extends VwParamError {
  def modifiedParams: Option[String] = None
  def errorMessage: String =
    "Encountered parameters that cannot be augmented: " +
      unrecoverable.toSeq.sorted.mkString(", ") +
      s"\n\toriginal parameters: $originalParams"
}

final case class NotCsoaaOrWap(originalParams: String) extends VwParamError {
  def modifiedParams: Option[String] = None
  def errorMessage: String =
    "Model must be a csoaa_ldf or wap_ldf model."
      s"\n\toriginal parameters: $originalParams"
}


final case class IncorrectLearner(
    originalParams: String,
    modifiedPs: String,
    learnerCanonicalName: String
) extends VwParamError {
  override def modifiedParams: Option[String] = Option(modifiedPs)
  override def errorMessage: String =
    s"Params produced an incorrect learner type.  " +
      s"Expected: ${classOf[ExpectedLearner].getCanonicalName}  " +
      s"Found: $learnerCanonicalName." +
      s"\n\toriginal parameters: $originalParams" +
      s"\n\tmodified parameters: $modifiedPs"
}

final case class ClassCastErr(
    originalParams: String,
    modifiedPs: String,
    ccException: ClassCastException
) extends VwParamError {
  override def modifiedParams: Option[String] = Option(modifiedPs)
  override def errorMessage: String =
    s"Params produced an incorrect learner type.  " +
      s"Expected: ${classOf[ExpectedLearner].getCanonicalName}  " +
      s"Encountered ClassCastException: ${ccException.getMessage}" +
      s"\n\toriginal parameters: $originalParams" +
      s"\n\tmodified parameters: $modifiedPs"
}

final case class VwError(
    originalParams: String,
    modifiedPs: String,
    vwErrMsg: String
) extends VwParamError {
  override def modifiedParams: Option[String] = Option(modifiedPs)
  override def errorMessage: String =
    s"VW could not create a learner of type " +
      s"${classOf[ExpectedLearner].getCanonicalName}. Error: $vwErrMsg. " +
      s"\n\toriginal parameters: $originalParams" +
      s"\n\tmodified parameters: $modifiedPs"
}

final case class NamespaceError(
    originalParams: String,
    namespaceNames: Set[String],
    flagsReferencingMissingNss: Map[String, Set[Char]]
) extends VwParamError {
  override def modifiedParams: Option[String] = None
  override def errorMessage: String = {
    val flagErrs =
      flagsReferencingMissingNss
        .toSeq.sortBy(_._1)
        .map { case(f, s) => s"--$f: ${s.mkString(",")}" }
        .mkString("; ")
    val nss = namespaceNames.toVector.sorted.mkString(", ")
    val vwNss = VwMultilabelModel.toVwNsSet(namespaceNames).toVector.sorted.mkString(",")

    s"Detected flags referencing namespaces not in the provided set. $flagErrs.  " +
      (
        if (vwNss.isEmpty) "No namespaces provided."
        else s"Expected only $vwNss from provided namespaces: $nss."
      ) +
      s"\n\toriginal parameters: $originalParams"
  }
}

final case class LabelNamespaceError(
    originalParams: String,
    namespaceNames: Set[String]
) extends VwParamError {
  override def modifiedParams: Option[String] = None
  override def errorMessage: String = {
    val nss = namespaceNames.toSeq.sorted.mkString(", ")
    s"Could not determine label namespaces from namespaces: $nss" +
      s"\n\toriginal parameters: $originalParams"
  }
}
