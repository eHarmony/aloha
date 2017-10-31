package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.File

import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator.LabelNamespaces
import com.eharmony.aloha.models.vw.jni.multilabel.VwSparseMultilabelPredictor.ExpectedLearner
import org.apache.commons.io.{FileUtils, IOUtils}
import vowpalWabbit.learner.VWLearners

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}


/**
  * Created by ryan.deak on 10/5/17.
  */
protected trait VwMultilabelParamAugmentation {

  protected type VWNsSet = Set[Char]
  protected type VWNsCrossProdSet = Set[(Char, Char)]

  /**
    * Adds VW parameters to make the parameters work as an Aloha multilabel model.
    *
    * The algorithm works as follows:
    *
    1. Ensure the VW `csoaa_ldf` or `wap_ldf` reduction is specified in the supplied VW
       parameter list (''with the appropriate option for the flag'').
    1. Ensure that no "''unrecoverable''" flags appear in the supplied VW parameter list.
       See `UnrecoverableFlagSet` for flags whose appearance is considered
       "''unrecoverable''".
    1. Ensure that ''ignore'' and ''interaction'' flags (`--ignore`, `--ignore_linear`, `-q`,
       `--quadratic`, `--cubic`) do not refer to namespaces not supplied in
       the `namespaceNames` parameter.
    1. Attempt to determine namespace names that can be used for the labels.  For more
       information on the label namespace resolution algorithm, see:
       `com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator.determineLabelNamespaces`.
    1. Remove flags and options found in `FlagsToRemove`.
    1. Add `--noconstant` and `--csoaa_rank` flags.  `--noconstant` is added because per-label
       intercepts will be included and take the place of a single intercept.  `--csoaa_rank`
       is added to make the `VWLearner` a `VWActionScoresLearner`.
    1. Create interactions between features and the label namespaces created above.
      a. If a namespace in `namespaceNames` appears as an option to VW's `ignore_linear` flag,
         '''do not''' create a quadratic interaction between that namespace and the label
         namespace.
      a. For each interaction term (`-q`, `--quadratic`, `--cubic`, `--interactions`), replace it
         with an interaction term also interacted with the label namespace.  This increases the
         arity of the interaction by 1.
    1. Finally, change the flag options that reference files to point to temp files so that
       VW doesn't change the files.  This may represent a problem if VW needs to read the file
       in the option because although it should exist, it will be empty.
    1. Let VW doing any validations it can.
    *
    * ==Success Examples==
    *
    * {{{
    * import com.eharmony.aloha.models.vw.jni.multilabel.VwMultilabelModel.updatedVwParams
    *
    * // This is a basic example. 'y' and 'Y' in the output are label
    * // namespaces.  Notice all namespaces are quadratically interacted
    * // with the label namespace.
    * val uvw1 = updatedVwParams(
    *   "--csoaa_ldf mc",
    *   Set("a", "b", "c")
    * )
    * // Right("--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
    * //       "--ignore_linear abc -qYa -qYb -qYc")
    *
    * // Here since 'a' is in 'ignore_linear', no '-qYa' term appears
    * // in the output.
    * val uvw2 = updatedVwParams(
    *   "--csoaa_ldf mc --ignore_linear a -qbc",
    *   Set("a", "b", "c")
    * )
    * // Right("--csoaa_ldf mc --noconstant --csoaa_rank --ignore y " +
    * //       "--ignore_linear abc -qYb -qYc --cubic Ybc)
    *
    * // 'a' is in 'ignore', so no terms with 'a' are emitted. 'b' is
    * // in 'ignore_linear' so it does occur in any quadratic
    * // interactions in the output, but can appear in interaction
    * // terms of higher arity like the cubic interaction.
    * val uvw3 = updatedVwParams(
    *   "--csoaa_ldf mc --ignore a --ignore_linear b -qbc --cubic abc",
    *   Set("a", "b", "c")
    * )
    * //  Right("--csoaa_ldf mc --noconstant --csoaa_rank --ignore ay " +
    * //        "--ignore_linear bc -qYc --cubic Ybc")
    * }}}
    *
    * ==Errors Examples==
    *
    * {{{
    * import com.eharmony.aloha.models.vw.jni.multilabel.VwMultilabelModel.updatedVwParams
    * import com.eharmony.aloha.models.vw.jni.multilabel.{
    *   NotCsoaaOrWap,
    *   NamespaceError
    * }
    *
    * assert( updatedVwParams("", Set())  ==  Left(NotCsoaaOrWap("")) )
    *
    * assert(
    *   updatedVwParams("--wap_ldf m -qaa", Set()) ==
    *   Left(NamespaceError("--wap_ldf m -qaa", Set(), Map("quadratic" -> Set('a'))))
    * )
    *
    * assert(
    *   updatedVwParams(
    *     "--wap_ldf m --ignore_linear b --ignore a -qbb -qbd " +
            "--cubic bcd --interactions dde --interactions abcde",
    *     Set()
    *   ) ==
    *   Left(
    *     NamespaceError(
    *       "--wap_ldf m --ignore_linear b --ignore a -qbb -qbd --cubic bcd " +
    *         "--interactions dde --interactions abcde",
    *       Set(),
    *       Map(
    *         "ignore"        -> Set('a'),
    *         "ignore_linear" -> Set('b'),
    *         "quadratic"     -> Set('b', 'd'),
    *         "cubic"         -> Set('b', 'c', 'd', 'e'),
    *         "interactions"  -> Set('a', 'b', 'c', 'd', 'e')
    *       )
    *     )
    *   )
    * )
    * }}}
    *
    * @param vwParams current VW parameters passed to the VW JNI
    * @param namespaceNames it is assumed that `namespaceNames` is a superset
    *                       of all of the namespaces referred to by any flags
    *                       found in `vwParams`.
    * @return
    */
  def updatedVwParams(vwParams: String, namespaceNames: Set[String]): Either[VwParamError, String] = {
    lazy val unrecovFlags = unrecoverableFlags(vwParams)

    if (WapOrCsoaa.findFirstMatchIn(vwParams).isEmpty)
      Left(NotCsoaaOrWap(vwParams))
    else if (unrecovFlags.nonEmpty)
      Left(UnrecoverableParams(vwParams, unrecovFlags))
    else {
      val is = interactions(vwParams)
      val i  = ignored(vwParams)
      val il = ignoredLinear(vwParams)

      // This won't effect anything if the definition of UnrecoverableFlags contains
      // all of the flags referenced in the flagsRefMissingNss function.  If there
      // are flags referenced in flagsRefMissingNss but not in UnrecoverableFlags,
      // then this is a valid check.
      val flagsRefMissingNss = flagsReferencingMissingNss(namespaceNames, i, il, is)

      if (flagsRefMissingNss.nonEmpty)
        Left(NamespaceError(vwParams, namespaceNames, flagsRefMissingNss))
      else
        VwMultilabelRowCreator.determineLabelNamespaces(namespaceNames).fold(
          Left(LabelNamespaceError(vwParams, namespaceNames)): Either[VwParamError, String]
        ){ labelNs =>
          val paramsWithoutRemoved = removeParams(vwParams)
          val updatedParams = addParams(paramsWithoutRemoved, namespaceNames, i, il, is, labelNs)

          val (finalParams, flagToFileMap) = replaceFileBasedFlags(updatedParams, FileBasedFlags)
          val ps = validateVwParams(
            vwParams, updatedParams, finalParams, flagToFileMap, !isQuiet(updatedParams)
          )
          flagToFileMap.values foreach FileUtils.deleteQuietly  // IO: Delete the files.
          ps
        }
    }
  }

  /**
    * VW Flags automatically resulting in an error.
    */
  protected val UnrecoverableFlagSet: Set[String] =
    Set("redefine", "stage_poly", "keep", "permutations", "autolink")

  /**
    * This is the capture group containing the content when the regex has been
    * padded with the pad function.
    */
  protected val CaptureGroupWithContent = 2

  private[this] val FileBasedFlags = Set(
    "-f", "--final_regressor",
    "--readable_model",
    "--invert_hash",
    "--output_feature_regularizer_binary",
    "--output_feature_regularizer_text",
    "-p", "--predictions",
    "-r", "--raw_predictions",
    "-c", "--cache",
    "--cache_file"
  )

  /**
    * Pad the regular expression with a prefix and suffix that makes matching work.
    * The prefix is `(^|\s)` and means the if there's a character preceding the main
    * content in `s`, then that character should be whitespace.  The suffix is
    * `(?=\s|$)` which means that if a character follows the main content matched by
    * `s`, then that character should be whitespace '''AND''' ''that character should
    * not'' be consumed by the Regex.  By allowing that character to be present for the
    * next matching of a regex, it is consumable by the prefix of a regex padded with
    * the `pad` function.
    * @param s a string
    * @return
    */
  private[this] def pad(s: String) = """(^|\s)""" + s + """(?=\s|$)"""
  private[this] val NumRegex            = """-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?"""
  private[this] val ClassCastMsg        = """(\S+) cannot be cast to (\S+)""".r
  private[this] val CsoaaRank           = pad("--csoaa_rank").r
  private[this] val WapOrCsoaa          = pad("""--(csoaa|wap)_ldf\s+(mc?)""").r
  private[this] val Quiet               = pad("--quiet").r
  protected     val Ignore      : Regex = pad("""--ignore\s+(\S+)""").r
  protected     val IgnoreLinear: Regex = pad("""--ignore_linear\s+(\S+)""").r
  private[this] val UnrecoverableFlags  = pad("--(" + UnrecoverableFlagSet.mkString("|") + ")").r
  private[this] val QuadraticsShort     = pad("""-q\s*([\S]{2})""").r
  private[this] val QuadraticsLong      = pad("""--quadratic\s+(\S{2})""").r
  private[this] val Cubics              = pad("""--cubic\s+(\S{3})""").r
  private[this] val Interactions        = pad("""--interactions\s+(\S{2,})""").r
  private[this] val NoConstant          = pad("""--noconstant""").r
  private[this] val ConstantShort       = pad("""-C\s*(""" + NumRegex + ")").r
  private[this] val ConstantLong        = pad("""--constant\s+(""" + NumRegex + ")").r


  private[this] val FlagsToRemove = Seq(
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

  /**
    * Remove flags (and options) for the flags listed in `FlagsToRemove`.
    * @param vwParams VW params passed to the `updatedVwParams` function.
    * @return
    */
  protected def removeParams(vwParams: String): String =
    FlagsToRemove.foldLeft(vwParams)((s, r) => r.replaceAllIn(s, ""))

  protected def addParams(
      paramsAfterRemoved: String,
      namespaceNames: Set[String],
      oldIgnored: VWNsSet,
      oldIgnoredLinear: VWNsSet,
      oldInteractions: Set[String],
      labelNs: LabelNamespaces
  ): String = {
    val i = oldIgnored + labelNs.dummyLabelNs

    // Don't include namespaces that are ignored in ignore_linear.
    val il = (toVwNsSet(namespaceNames) ++ oldIgnoredLinear) -- i

    // Don't turn a given namespace into quadratics interacted on label when the
    // namespace is listed in the ignore_linear flag.
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

    // This is non-empty b/c i is non-empty.
    val ig = s"--ignore ${i.mkString("")}"

    // Consolidate whitespace because there shouldn't be whitespace in these flags' options.
    val additions = s" --noconstant --csoaa_rank $ig $igLin $quadratics $cubics $ints"
        .replaceAll("\\s+", " ")
    (paramsAfterRemoved.trim + additions).trim
  }

  /**
    * VW will actually update / replace files if files appear as options to flags.  To overcome
    * this, an attempt is made to detect flags referencing files and if found, replace the the
    * files with temp files.  These files should be deleted before exiting the main program.
    * @param updatedParams the parameters after the updates.
    * @param flagsWithFiles the flag
    * @return a tuple2 of the final string to try with VW for validation along with the mapping
    *         from flag to file that was used.
    */
  protected def replaceFileBasedFlags(updatedParams: String, flagsWithFiles: Set[String]): (String, Map[String, File]) = {
    // This is rather hairy function.

    def flagRe(flags: Set[String], groupsForFlag: Int, c1: String, c2: String, c3: String) =
      if (flags.nonEmpty)
        Option(pad(flags.map(_ drop groupsForFlag).toVector.sorted.mkString(c1, c2, c3)).r)
      else None

    // Get short and long flags.
    val shrt = flagsWithFiles.filter(s => s.startsWith("-") && 2 == s.length && s.charAt(1).isLetterOrDigit)
    val lng = flagsWithFiles.filter(s => s.startsWith("--") && 2 < s.length)

    val regexes = List(
      flagRe(shrt, 1, "(-[", "", """])\s*(\S+)"""),
      flagRe(lng,  2, "(--(", "|", """))\s+(\S+)""")
    ).flatten

    regexes.foldLeft((updatedParams, Map[String, File]())) { case ((ps, ffm), r) =>
      // Fold right to not affect subsequent replacements.
      r.findAllMatchIn(ps).foldRight((ps, ffm)) { case (m, (ps1, ffm1)) =>
        val f = File.createTempFile("REPLACED_", "_FILE")
        f.deleteOnExit() // Attempt to be safe here.

        val flag = m.group(CaptureGroupWithContent)
        val rep = s"$flag ${f.getCanonicalPath}"
        val ps2 = ps1.take(m.start) + rep + ps1.drop(m.end)
        val ffm2 = ffm1 + (flag -> f)
        (ps2, ffm2)
      }
    }
  }

  protected def createLabelInteractions(
      interactions: Set[String],
      ignored: VWNsSet,
      labelNs: LabelNamespaces,
      filter: Int => Boolean
  ): Set[String] =
    interactions.collect {
      case i if filter(i.length) &&                         // Filter based on arity.
                !i.toCharArray.exists(ignored.contains) =>  // Filter out ignored.
        s"${labelNs.labelNs}$i"
    }

  /**
    * Get the set of interactions (encoded as Strings).  String length represents the
    * interaction arity.
    * @param vwParams VW params passed to the `updatedVwParams` function.
    * @return
    */
  protected def interactions(vwParams: String): Set[String] =
    List(
      QuadraticsShort,
      QuadraticsLong,
      Cubics,
      Interactions
    ).foldLeft(Set.empty[String]){(is, r) =>
      is ++ firstCaptureGroups(vwParams, r).map(s => s.sorted)
    }

  protected def unrecoverableFlags(vwParams: String): Set[String] =
    firstCaptureGroups(vwParams, UnrecoverableFlags).toSet

  protected def isQuiet(vwParams: String): Boolean = Quiet.findFirstIn(vwParams).nonEmpty
  protected def ignored(vwParams: String): VWNsSet = charsIn(Ignore, vwParams)
  protected def ignoredLinear(vwParams: String): VWNsSet = charsIn(IgnoreLinear, vwParams)

  protected def handleClassCastException(
      orig: String,
      mod: String,
      ex: ClassCastException
  ): VwParamError =
    ex.getMessage match {
      case ClassCastMsg(from, _) => IncorrectLearner(orig, mod, from)
      case _                     => ClassCastErr(orig, mod, ex)
    }

  protected def flagsReferencingMissingNss(
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

  // TODO: Change file
  protected def validateVwParams(
      orig: String,
      mod: String,
      finalPs: String,
      flagToFileMap: Map[String, File],
      addQuiet: Boolean
  ): Either[VwParamError, String] = {
    val ps = if (addQuiet) s"--quiet $finalPs" else finalPs

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

  // More general functions.

  /**
    * Find all of the regex matches and extract the first capture group from the match.
    * @param vwParams VW params passed to the `updatedVwParams` function.
    * @param regex with at least one capture group (this is unchecked).
    * @return Iterator of the matches' first capture group.
    */
  protected def firstCaptureGroups(vwParams: String, regex: Regex): Iterator[String] =
    regex.findAllMatchIn(vwParams).map(m => m.group(CaptureGroupWithContent))

  protected def charsIn(r: Regex, chrSeq: CharSequence): VWNsSet =
    r.findAllMatchIn(chrSeq).flatMap(m => m.group(CaptureGroupWithContent).toCharArray).toSet

  private[multilabel] def toVwNsSet(nsNames: Set[String]): VWNsSet =
    nsNames.flatMap(_.take(1).toCharArray)
}
