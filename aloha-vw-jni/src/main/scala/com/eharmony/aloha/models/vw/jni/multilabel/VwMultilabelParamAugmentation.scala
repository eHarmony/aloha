package com.eharmony.aloha.models.vw.jni.multilabel

import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator.LabelNamespaces
import com.eharmony.aloha.models.vw.jni.multilabel.VwSparseMultilabelPredictor.ExpectedLearner
import org.apache.commons.io.IOUtils
import vowpalWabbit.learner.VWLearners

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex


/**
  * Created by ryan.deak on 10/5/17.
  */
protected trait VwMultilabelParamAugmentation {

  protected type VWNsSet = Set[Char]
  protected type VWNsCrossProdSet = Set[(Char, Char)]

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
    lazy val unrecovFlags = unrecoverableFlags(padded)

    if (WapOrCsoaa.findFirstMatchIn(padded).isEmpty)
      Left(NotCsoaaOrWap(vwParams))
    else if (unrecovFlags.nonEmpty)
      Left(UnrecoverableParams(vwParams, unrecovFlags))
    else {
      val is = interactions(padded)
      val i  = ignored(padded)
      val il = ignoredLinear(padded)

      // This won't effect anything if the definition of UnrecoverableFlags contains
      // all of the flags referenced in the flagsRefMissingNss function.  If there
      // are flags referenced in flagsRefMissingNss but not in UnrecoverableFlags,
      // then this is a valid check.
      val flagsRefMissingNss = flagsReferencingMissingNss(padded, namespaceNames, i, il, is)

      if (flagsRefMissingNss.nonEmpty)
        Left(NamespaceError(vwParams, namespaceNames, flagsRefMissingNss))
      else
        VwMultilabelRowCreator.determineLabelNamespaces(namespaceNames).fold(
          Left(LabelNamespaceError(vwParams, namespaceNames)): Either[VwParamError, String]
        ){ labelNs =>
          val paramsWithoutRemoved = removeParams(padded)
          val updatedParams = addParams(paramsWithoutRemoved, namespaceNames, i, il, is, labelNs)
          validateVwParams(vwParams, updatedParams, !isQuiet(updatedParams))
        }
    }
  }

  protected val UnrecoverableFlagSet: Set[String] =
    Set("redefine", "stage_poly", "keep", "permutations")

  private[this] def pad(s: String) = "\\s" + s + "\\s"
  private[this] val NumRegex            = """-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?"""
  private[this] val ClassCastMsg        = """(\S+) cannot be cast to (\S+)""".r
  private[this] val CsoaaRank           = pad("--csoaa_rank").r
  private[this] val WapOrCsoaa          = pad("""--(csoaa|wap)_ldf\s+(mc?)""").r
  private[this] val Quiet               = pad("--quiet").r
  private[this] val Keep                = pad("""--keep\s+(\S+)""").r
  protected     val Ignore      : Regex = pad("""--ignore\s+(\S+)""").r
  protected     val IgnoreLinear: Regex = pad("""--ignore_linear\s+(\S+)""").r
  private[this] val UnrecoverableFlags  = pad("--(" + UnrecoverableFlagSet.mkString("|") + ")").r
  private[this] val QuadraticsShort     = pad("""-q\s*([\S]{2})""").r
  private[this] val QuadraticsLong      = pad("""--quadratic\s+(\S{2})""").r
  private[this] val Cubics              = pad("""--cubic\s+(\S{3})""").r
  private[this] val Interactions        = pad("""--interactions\s+(\S\S+)""").r
  private[this] val NoConstant          = pad("""--noconstant""").r
  private[this] val ConstantShort       = pad("""-C\s*(""" + NumRegex + ")").r
  private[this] val ConstantLong        = pad("""--constant\s+(""" + NumRegex + ")").r

  private[this] val OptionsRemoved = Seq(
    QuadraticsShort,
    QuadraticsLong,
    Cubics,
    Interactions,
    NoConstant,
    ConstantShort,
    ConstantLong,
    CsoaaRank,
    IgnoreLinear,
    Ignore
  )

  protected def removeParams(padded: String): String = {
    @tailrec def replaceAll(s: String, r: Regex): String = {
      val str = r.replaceAllIn(s, " ").trim
      s" $str " match {
        case v if v == s => v
        case v           => replaceAll(v, r)
      }
    }

    // TODO: Figure out why Regex.replaceAllIn doesn't replace all.
    OptionsRemoved.foldLeft(padded)((s, r) => replaceAll(s, r))
  }

  protected def addParams(
      paramsAfterRemoved: String,
      namespaceNames: Set[String],
      oldIgnored: VWNsSet,
      oldIgnoredLinear: VWNsSet,
      oldInteractions: Set[String],
      labelNs: LabelNamespaces
  ): String = {
    val i = oldIgnored + labelNs.dummyLabelNs
    val il = (toVwNsSet(namespaceNames) ++ oldIgnoredLinear) -- i
    val qs = il.flatMap(n =>
      if (oldIgnored.contains(n) || oldIgnoredLinear.contains(n)) Nil
      else List(s"${labelNs.labelNs}$n")
    )

    // Turn quadratic into cubic and cubic into higher-order interactions.
    val cs  = createLabelInteractions(oldInteractions, oldIgnored, labelNs, _ == 2)
    val hos = createLabelInteractions(oldInteractions, oldIgnored, labelNs, _ >= 3)

    val quadratics = qs.toSeq.sorted.map(q => s"-q$q" ).mkString(" ")
    val cubics = cs.toSeq.sorted.map(c => s"--cubic $c").mkString(" ")
    val ints = hos.toSeq.sorted.map(ho => s"--interactions $ho").mkString(" ")
    val igLin = if (il.nonEmpty) il.toSeq.sorted.mkString("--ignore_linear ", "", "") else ""
    val ig = s"--ignore ${i.mkString("")}"
    val additions = s" --noconstant --csoaa_rank $ig $igLin $quadratics $cubics $ints"
        .replaceAll("\\s+", " ")
    (paramsAfterRemoved.trim + additions).trim
  }

  protected def createLabelInteractions(
      interactions: Set[String],
      ignored: VWNsSet,
      labelNs: LabelNamespaces,
      filter: Int => Boolean
  ): Set[String] =
    interactions.collect {
      case i if filter(i.length) && !i.toCharArray.exists(ignored.contains) => s"${labelNs.labelNs}$i"
    }

  protected def interactions(padded: String): Set[String] = {
    List(
      QuadraticsShort,
      QuadraticsLong,
      Cubics,
      Interactions
    ).foldLeft(Set.empty[String]){(is, r) =>
      is ++ firstGroups(padded, r).map(s => s.sorted)
    }
  }

  protected def firstGroups(padded: String, regex: Regex): Iterator[String] =
    regex.findAllMatchIn(padded).map(m => m.group(1))

//  protected def setOfFirstGroup(padded: String, regex: Regex): Set[String] =
//    regex.findAllMatchIn(padded).map(m => m.group(1)).toSet

//  protected def setOfFirstGroups(padded: String, regexs: Regex*): Set[String] =
//    regexs.aggregate(Set.empty[String])((_, r) => setOfFirstGroup(padded, r), _ ++ _)

  protected def noconstant(padded: String): Boolean =
    NoConstant.findFirstIn(padded).nonEmpty

//  protected def constant(padded: String): Set[Double] =
//    setOfFirstGroups(padded, ConstantShort, ConstantLong).map(s => s.toDouble)

  protected def unorderedCross[A](a: A, b: A)(implicit o: Ordering[A]): (A, A) =
    if (o.lt(a, b)) (a, b) else (b, a)

  protected def firstThenOrderedCross[A](first: A, b: A, c: A)(implicit o: Ordering[A]): (A, A, A) = {
    val (x, y) = unorderedCross(b, c)
    (first, x, y)
  }

  protected def quad(r: Regex, chrSeq: CharSequence): VWNsCrossProdSet =
    r.findAllMatchIn(chrSeq).map { m =>
      val Seq(a, b) = (1 to 2).map(i => m.group(i).charAt(0))
      unorderedCross(a, b)
    }.toSet

  protected def charsIn(r: Regex, chrSeq: CharSequence): VWNsSet =
    r.findAllMatchIn(chrSeq).flatMap(m => m.group(1).toCharArray).toSet

  protected def unrecoverableFlags(padded: String): Set[String] =
    UnrecoverableFlags.findAllMatchIn(padded).map(m => m.group(1)).toSet

  protected def quadratics(padded: String): VWNsCrossProdSet =
    quad(QuadraticsShort, padded) ++ quad(QuadraticsLong, padded)

  protected def crossToSet[A](cross: Set[(A, A)]): Set[A] =
    cross flatMap { case (a, b) => Set(a, b) }

  protected def isQuiet(padded: String): Boolean = Quiet.findFirstIn(padded).nonEmpty

//  protected def kept(padded: String): VWNsSet = charsIn(Keep, padded)
  protected def ignored(padded: String): VWNsSet = charsIn(Ignore, padded)
  protected def ignoredLinear(padded: String): VWNsSet = charsIn(IgnoreLinear, padded)

  protected def handleClassCastException(orig: String, mod: String, ex: ClassCastException): VwParamError =
    ex.getMessage match {
      case ClassCastMsg(from, _) => IncorrectLearner(orig, mod, from)
      case _                     => ClassCastErr(orig, mod, ex)
    }

  protected def flagsReferencingMissingNss(
      padded: String,
      namespaceNames: Set[String],
      i: VWNsSet,
      il: VWNsSet,
      is: Set[String]
  ): Map[String, VWNsSet] = {
    val q  = filterAndFlattenInteractions(is, _ == 2)
    val c  = filterAndFlattenInteractions(is, _ == 3)
    val ho = filterAndFlattenInteractions(is, _ >= 4)
    flagsReferencingMissingNss(namespaceNames, i, il, q, c, ho)
  }

  protected def filterAndFlattenInteractions(is: Set[String], filter: Int => Boolean): VWNsSet =
    is.flatMap {
      case interaction if filter(interaction.length) => interaction.toCharArray
      case _ => Nil
    }

  protected def flagsReferencingMissingNss(
      namespaceNames: Set[String],
      i: VWNsSet, il: VWNsSet, q: VWNsSet, c: VWNsSet, ho: VWNsSet
  ): Map[String, VWNsSet] =
    nssNotInNamespaceNames(
      namespaceNames,
      "ignore"        -> i,
      "ignore_linear" -> il,
      "quadratic"     -> q,
      "cubic"         -> c,
      "interactions"  -> ho
    )

  protected def nssNotInNamespaceNames(
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

  private[multilabel] def toVwNsSet(nsNames: Set[String]): VWNsSet =
    nsNames.flatMap(_.take(1).toCharArray)

  protected def validateVwParams(orig: String, mod: String, addQuiet: Boolean): Either[VwParamError, String] = {
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
