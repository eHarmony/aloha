package com.eharmony.aloha.models.vw.jni.multilabel

import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator.LabelNamespaces
import com.eharmony.aloha.models.vw.jni.multilabel.VwSparseMultilabelPredictor.ExpectedLearner
import org.apache.commons.io.IOUtils
import vowpalWabbit.learner.VWLearners

import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex


/**
  * Created by ryan.deak on 10/5/17.
  */
private[multilabel] trait VwMultilabelParamAugmentation {

  private[multilabel] type VWNsSet = Set[Char]
  private[multilabel] type VWNsCrossProdSet = Set[(Char, Char)]


  /**
    * Add VW parameters to make the multilabel model work:
    *
    *
    * @param vwParams current VW parameters passed to the VW JNI
    * @param namespaceNames it is assumed that `namespaceNames` is a superset
    *                       of all of the namespaces referred to by any
    * @return
    */
  def updatedVwParams(vwParams: String, namespaceNames: Set[String]): Either[VwParamError, String] = {
    val padded = s" ${vwParams.trim} "
    val unrecovFlags = unrecoverableFlags(padded)
    if (unrecovFlags.nonEmpty)
      Left(UnrecoverableParams(vwParams, unrecovFlags))
    else {
      val q = quadratics(padded)  // Turn these into cubic features later.

      // This won't effect anything if the definition of UnrecoverableFlags contains
      // all of the flags referenced in the flagsRefMissingNss function.  If there
      // are flags referenced in flagsRefMissingNss but not in UnrecoverableFlags,
      // then this is a valid check.
      val flagsRefMissingNss = flagsReferencingMissingNss(padded, namespaceNames, q)

      if (flagsRefMissingNss.nonEmpty)
        Left(NamespaceError(vwParams, namespaceNames, flagsRefMissingNss))
      else
        VwMultilabelRowCreator.determineLabelNamespaces(namespaceNames).fold(
          Left(LabelNamespaceError(vwParams, namespaceNames)): Either[VwParamError, String]
        ){ labelNs =>
          val paramsWithoutRemoved = removeParams(padded)
          val updatedParams = addParams(paramsWithoutRemoved, namespaceNames, q, labelNs)
          validateVwParams(vwParams, updatedParams, !isQuiet(updatedParams))
        }
    }
  }

  private[this] def pad(s: String) = "\\s" + s + "\\s"
  private[this] val NumRegex            = """-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?"""
  private[this] val ClassCastMsg        = """(\S+) cannot be cast to (\S+)""".r
  private[this] val CsoaaRank           = pad("--csoaa_rank").r
  private[this] val CsoaaLdf            = pad("""--csoaa_ldf\s+(mc?)""").r
  private[this] val CsoaaRegression     = "m"
  private[this] val CsoaaClassification = "mc"
  private[this] val Quiet               = pad("--quiet").r
  private[this] val Keep                = pad("""--keep\s+(\S+)""").r
  private[this] val Ignore              = pad("""--ignore\s+(\S+)""").r
  private[this] val IgnoreLinear        = pad("""--ignore_linear\s+(\S+)""").r
  private[multilabel] val UnrecoverableFlagSet =
    Set("cubic", "redefine", "stage_poly", "ignore", "ignore_linear", "keep")
  private[this] val UnrecoverableFlags  = pad("--(" + UnrecoverableFlagSet.mkString("|") + ")").r
  private[this] val QuadraticsShort     = pad("""-q\s*([\S])([\S])""").r
  private[this] val QuadraticsLong      = pad("""--quadratic\s+([\S])([\S])""").r
  private[this] val NoConstant          = pad("""--noconstant""").r
  private[this] val ConstantShort       = pad("""-C\s*(""" + NumRegex + ")").r
  private[this] val ConstantLong        = pad("""--constant\s+(""" + NumRegex + ")").r

  private[this] val OptionsRemoved = Seq(
    QuadraticsShort,
    QuadraticsLong,
    NoConstant,
    ConstantShort,
    ConstantLong,
    CsoaaRank
  )

  private[multilabel] def removeParams(padded: String): String =
    OptionsRemoved.foldLeft(padded){(s, r) =>
      val v = r.replaceAllIn(s, " ")
      println(s"after  $r:".padTo(70, ' ') + v)
      v
    }

  private[multilabel] def addParams(
      paramsAfterRemoved: String,
      namespaceNames: Set[String],
      oldQuadratics: VWNsCrossProdSet,
      labelNs: LabelNamespaces
  ): String = {
    val il = toVwNsSet(namespaceNames)
    val q  = il.map(n => (labelNs.labelNs, n))
    val c  = oldQuadratics.map { case (a, b) => firstThenOrderedCross(labelNs.labelNs, a, b) }

    val quadratics = q.toSeq.sorted.map{ case (y, x) => s"-q$y$x" }.mkString(" ")
    val cubics = c.toSeq.sorted.map{ case (y, x1, x2) => s"--cubic $y$x1$x2" }.mkString(" ")
    val igLin = il.toSeq.sorted.map(n => s"--ignore_linear $n").mkString(" ")
    val ig = s"--ignore ${labelNs.dummyLabelNs}"

    (paramsAfterRemoved.trim + s" --noconstant --csoaa_rank $ig $igLin $quadratics $cubics").trim
  }

  private[multilabel] def setOfFirstGroup(padded: String, regex: Regex): Set[String] =
    regex.findAllMatchIn(padded).map(m => m.group(1)).toSet

  private[multilabel] def setOfFirstGroups(padded: String, regexs: Regex*): Set[String] =
    regexs.aggregate(Set.empty[String])((_, r) => setOfFirstGroup(padded, r), _ ++ _)

  private[multilabel] def noconstant(padded: String): Boolean =
    NoConstant.findFirstIn(padded).nonEmpty

  private[multilabel] def constant(padded: String): Set[Double] =
    setOfFirstGroups(padded, ConstantShort, ConstantLong).map(s => s.toDouble)

  private[multilabel] def unorderedCross[A](a: A, b: A)(implicit o: Ordering[A]) =
    if (o.lt(a, b)) (a, b) else (b, a)

  private[multilabel] def firstThenOrderedCross[A](first: A, b: A, c: A)(implicit o: Ordering[A]) = {
    val (x, y) = unorderedCross(b, c)
    (first, x, y)
  }

  private[multilabel] def quad(r: Regex, chrSeq: CharSequence): VWNsCrossProdSet =
    r.findAllMatchIn(chrSeq).map { m =>
      val Seq(a, b) = (1 to 2).map(i => m.group(i).charAt(0))
      unorderedCross(a, b)
    }.toSet

  private[multilabel] def charsIn(r: Regex, chrSeq: CharSequence): VWNsSet =
    r.findAllMatchIn(chrSeq).flatMap(m => m.group(1).toCharArray).toSet

  private[multilabel] def unrecoverableFlags(padded: String): Set[String] =
    UnrecoverableFlags.findAllMatchIn(padded).map(m => m.group(1)).toSet

  private[multilabel] def quadratics(padded: String): VWNsCrossProdSet =
    quad(QuadraticsShort, padded) ++ quad(QuadraticsLong, padded)

  private[multilabel] def crossToSet[A](cross: Set[(A, A)]): Set[A] =
    cross flatMap { case (a, b) => Set(a, b) }

  private[multilabel] def isQuiet(padded: String): Boolean = Quiet.findFirstIn(padded).nonEmpty

  private[multilabel] def kept(padded: String): VWNsSet = charsIn(Keep, padded)
  private[multilabel] def ignored(padded: String): VWNsSet = charsIn(Ignore, padded)
  private[multilabel] def ignoredLinear(padded: String): VWNsSet = charsIn(IgnoreLinear, padded)

  private[multilabel] def handleClassCastException(orig: String, mod: String, ex: ClassCastException) =
    ex.getMessage match {
      case ClassCastMsg(from, _) => IncorrectLearner(orig, mod, from)
      case _                     => ClassCastErr(orig, mod, ex)
    }

  private[multilabel] def flagsReferencingMissingNss(
      padded: String,
      namespaceNames: Set[String],
      q: VWNsCrossProdSet
  ): Map[String, VWNsSet] = {
    val nsq = crossToSet(q)
    val k   = kept(padded)
    val i   = ignored(padded)
    val il  = ignoredLinear(padded)
    flagsReferencingMissingNss(namespaceNames, k, i, il, nsq)
  }

  private[multilabel] def flagsReferencingMissingNss(
      namespaceNames: Set[String],
      k: VWNsSet, i: VWNsSet, il: VWNsSet, nsq: VWNsSet
  ): Map[String, VWNsSet] =
    nssNotInNamespaceNames(
      namespaceNames,
      "keep"          -> k,
      "ignore"        -> i,
      "ignore_linear" -> il,
      "quadratic"     -> nsq
    )

  private[multilabel] def toVwNsSet(nsNames: Set[String]): VWNsSet =
    nsNames.flatMap(_.take(1).toCharArray)

  private[multilabel] def nssNotInNamespaceNames(
      nsNames: Set[String],
      sets: (String, VWNsSet)*
  ): Map[String, VWNsSet] = {
    val vwNss = toVwNsSet(nsNames)

    sets.foldLeft(Map.empty[String, VWNsSet]){ case (m, (setName, nss)) =>
      val extra = nss diff vwNss
      if (extra.isEmpty) m
      else m + (setName -> extra)
    }
  }

  private[multilabel] def validateVwParams(orig: String, mod: String, addQuiet: Boolean) = {
    val ps = if (addQuiet) s"--quiet $mod" else mod

    Try { VWLearners.create[ExpectedLearner](ps) } match {
      case Success(m) =>
        IOUtils.closeQuietly(m)
        Right(mod)
      case Failure(cce: ClassCastException) =>
        Left(handleClassCastException(orig, mod, cce))
      case Failure(ex) =>
        Left(VwError(orig, mod, ex.getMessage))
    }
  }
}
