package com.eharmony.aloha.dataset.vw.multilabel

import com.eharmony.aloha.dataset._
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.vw.VwCovariateProducer
import com.eharmony.aloha.dataset.vw.multilabel.json.VwMultilabeledJson
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.rand.Rand
import spray.json.JsValue

import scala.collection.{breakOut, immutable => sci}
import scala.util.Try

/**
  * Creates training data for multilabel models in Vowpal Wabbit's CSOAA LDF and WAP LDF format
  * for the JNI.
  *
  * @param allLabelsInTrainingSet all labels in the training set.  This is a sequence because
  *                               order matters.  Order here can be chosen arbitrarily, but it
  *                               must be consistent in the training and test formulation.
  * @param featuresFunction features to extract from the data of type `A`.
  * @param defaultNamespace list of feature indices in the default VW namespace.
  * @param namespaces a mapping from VW namespace name to feature indices in that namespace.
  * @param normalizer can modify VW output (currently unused)
  * @param positiveLabelsFunction A method that can extract positive class labels.
  * @param classNs the namespace name for class information.
  * @param dummyClassNs the namespace name for dummy class information.  2 dummy classes are
  *                     added to make the predicted probabilities work.
  * @param includeZeroValues include zero values in VW input?
  * @tparam A the input type
  * @tparam K the label or class type
  * @author deaktator
  * @since 9/13/2017
  */
final case class VwMultilabelRowCreator[-A, K](
    allLabelsInTrainingSet: sci.IndexedSeq[K],
    featuresFunction: FeatureExtractorFunction[A, Sparse],
    defaultNamespace: List[Int],
    namespaces: List[(String, List[Int])],
    normalizer: Option[CharSequence => CharSequence],
    positiveLabelsFunction: GenAggFunc[A, sci.IndexedSeq[K]],
    classNs: Char,
    dummyClassNs: Char,
    includeZeroValues: Boolean = false)
extends RowCreator[A, Array[String]] {
  import VwMultilabelRowCreator._

  @transient private[this] lazy val labelToInd = allLabelsInTrainingSet.zipWithIndex.toMap

  // Precompute these for efficiency rather recompute than inside a hot loop.
  // Notice these are not lazy vals.

  private[this] val negativeDummyStr =
    s"$NegDummyClassId:$NegativeCost |$dummyClassNs $NegativeDummyClassFeature"

  private[this] val positiveDummyStr =
    s"$PosDummyClassId:$PositiveCost |$dummyClassNs $PositiveDummyClassFeature"

  override def apply(a: A): (MissingAndErroneousFeatureInfo, Array[String]) = {
    val (missingAndErrs, features) = featuresFunction(a)

    // Get the lazy val once.
    val labToInd = labelToInd

    // TODO: Should this be sci.BitSet?
    val positiveIndices: Set[Int] = positiveLabelsFunction(a).flatMap(labToInd.get)(breakOut)

    val x: Array[String] = trainingInput(
      features,
      allLabelsInTrainingSet.indices,
      positiveIndices,
      defaultNamespace,
      namespaces,
      classNs,
      negativeDummyStr,
      positiveDummyStr
    )

    (missingAndErrs, x)
  }
}

object VwMultilabelRowCreator extends Rand {

  /**
    * VW allows long-based feature indices, but Aloha only allow's 32-bit indices
    * on the features that produce the key-value pairs passed to VW. The negative
    * dummy classes uses an ID outside of the allowable range of feature indices:
    * 2^32^.
    */
  private[multilabel] val NegDummyClassId = (Int.MaxValue.toLong + 1L).toString

  /**
    * VW allows long-based feature indices, but Aloha only allow's 32-bit indices
    * on the features that produce the key-value pairs passed to VW. The positive
    * dummy classes uses an ID outside of the allowable range of feature indices:
    * 2^32^ + 1.
    */
  private[multilabel] val PosDummyClassId = (Int.MaxValue.toLong + 2L).toString

  // NOTE: If PositiveCost and NegativeCost change,
  //       VwDownsampledMultilabledRowCreator.sampledTrainingInput
  //       will also need to change.

  /**
    * Since VW CSOAA stands for '''COST''' ''Sensitive One Against All'', the
    * dependent variable is based on cost (which is the negative of reward).
    * As such, the ''reward'' of a positive example is designated to be zero.
    */
  private[multilabel] val PositiveCost = 0

  /**
    * Since VW CSOAA stands for '''COST''' ''Sensitive One Against All'', the
    * dependent variable is based on cost (which is the negative of reward).
    * As such, the ''reward'' of a negative example is designated to be -1,
    * so the cost (or negative reward) is 1.
    */
  private[multilabel] val NegativeCost = 1

  private[multilabel] val PositiveDummyClassFeature = "P"

  private[multilabel] val NegativeDummyClassFeature = "N"

  /**
    * "shared" is a special keyword in VW multi-class (multi-row) format.
    * See Hal Daume's [[https://www.umiacs.umd.edu/%7Ehal/tmp/multiclassVW.html page]].
    *
    * '''NOTE''': The trailing space should be here.
    */
  private[multilabel] val SharedFeatureIndicator = "shared" + " "

  private[this] val PreferredLabelNamespaces = Seq(('Y', 'y'), ('Z', 'z'), ('Λ', 'λ'))

  /**
    * Determine the label namespaces for VW.  VW insists on the uniqueness of the first character
    * of namespaces.  The goal is to use try to use the first one of these combinations for
    * label and dummy label namespaces where neither of the values are in `usedNss`.
    *
    - "Y", "y"
    - "Z", "z"
    - "Λ", "λ"
    *
    * If one of these combinations cannot be used because at least one of the elements in a given
    * row is in `usedNss`, then iterate over the Unicode set and take the first two characters
    * found that adhere to are deemed a valid character.  These will then become the actual and
    * dummy namespace names (respectively).
    *
    * The goal of this function is to try to use characters in literature used to denote a
    * dependent variable.  If that isn't possible (because the characters are already used by some
    * other namespace), just find the first possible characters.
    * @param usedNss names of namespaces used.
    * @return the namespace for ''actual'' label information then the namespace for ''dummy''
    *         label information.  If two valid namespaces couldn't be produced, return None.
    */
  private[aloha] def determineLabelNamespaces(usedNss: Set[String]): Option[LabelNamespaces] = {
    val nss = nssToFirstCharBitSet(usedNss)
    preferredLabelNamespaces(nss) orElse bruteForceNsSearch(nss)
  }

  private[multilabel] def preferredLabelNamespaces(nss: sci.BitSet): Option[LabelNamespaces] = {
    PreferredLabelNamespaces collectFirst {
      case (actual, dummy) if !(nss contains actual.toInt) && !(nss contains dummy.toInt) =>
        LabelNamespaces(actual, dummy)
    }
  }

  private[multilabel] def nssToFirstCharBitSet(ss: Set[String]): sci.BitSet =
    ss.collect { case s if s.length != 0 =>
      s.charAt(0).toInt
    }(breakOut[Set[String], Int, sci.BitSet])

  private[multilabel] def validCharForNamespace(chr: Char): Boolean = {
    // These might be overkill.
    Character.isDefined(chr) &&
    Character.isLetter(chr) &&
    !Character.isISOControl(chr) &&
    !Character.isSpaceChar(chr) &&
    !Character.isWhitespace(chr)
  }

  /**
    * Find the first two valid characters that can be used as VW namespaces that, when converted
    * to integers are not present in usedNss.
    * @param usedNss the set of first characters in namespaces.
    * @return the namespace to use for the actual classes and dummy classes, respectively.
    */
  private[multilabel] def bruteForceNsSearch(usedNss: sci.BitSet): Option[LabelNamespaces] = {
    val found =
      Iterator
        .range(Char.MinValue, Char.MaxValue)
        .filter(c => !(usedNss contains c) && validCharForNamespace(c.toChar))
        .take(2)
        .toList

    found match {
      case actual :: dummy :: Nil =>
        Option(LabelNamespaces(actual.toChar, dummy.toChar))
      case _ => None
    }
  }

  /**
    * Produce a multi-line input to be consumed by the underlying ''CSOAA LDF'' VW model.
    * @param features (non-label dependent) features shared across all labels.
    * @param indices the indices `labels` into the sequence of all labels encountered
    *                during training.
    * @param positiveLabelIndices a predicate telling whether the example should be positively
    *                             associated with a label.
    * @param defaultNs the indices into `features` that should be placed in VW's default
    *                  namespace.
    * @param namespaces the indices into `features` that should be associated with each
    *                   namespace.
    * @param classNs a namespace for features associated with class labels
    * // @param dummyClassNs a namespace for features associated with dummy class labels
    * @param negativeDummyStr
    * @param positiveDummyStr
    * @return an array to be passed directly to an underlying `VWActionScoresLearner`.
    */
  private[multilabel] def trainingInput(
      features: IndexedSeq[Sparse],
      indices: sci.IndexedSeq[Int],
      positiveLabelIndices: Int => Boolean,
      defaultNs: List[Int],
      namespaces: List[(String, List[Int])],
      classNs: Char,
      negativeDummyStr: String,
      positiveDummyStr: String
  ): Array[String] = {

    val n = indices.size

    // The length of the output array is n + 3.
    //
    // The first row is the shared features. These are features that are not label dependent.
    // Then comes two dummy classes.  These are to make the probabilities work out.
    // Then come the features for each of the n labels.
    val x = new Array[String](n + 3)

    val shared = VwRowCreator.unlabeledVwInput(features, defaultNs, namespaces, includeZeroValues = false)
    x(0) = SharedFeatureIndicator + shared

    // These string interpolations are computed over and over but will always be the same
    // for a given dummyClassNs.
    x(1) = negativeDummyStr
    x(2) = positiveDummyStr

    // vvvvv  This is mutable because we want speed.  vvvvv

    var i = 0
    while (i < n) {
      val labelInd = indices(i)

      // TODO or positives.contains(labelInd)?
      val dv = if (positiveLabelIndices(i)) PositiveCost else NegativeCost
      x(i + 3) = s"$labelInd:$dv |$classNs _$labelInd"
      i += 1
    }

    x
  }

  /**
    * Produce a multi-line input to be consumed by the underlying ''CSOAA LDF'' VW model.
    * @param features (non-label dependent) features shared across all labels.
    * @param indices the indices `labels` into the sequence of all labels encountered
    *                during training.
    * @param defaultNs the indices into `features` that should be placed in VW's default
    *                  namespace.
    * @param namespaces the indices into `features` that should be associated with each
    *                   namespace.
    * @param classNs a namespace for features associated with class labels
    * @return an array to be passed directly to an underlying `VWActionScoresLearner`.
    */
  private[aloha] def predictionInput(
      features: IndexedSeq[Sparse],
      indices: sci.IndexedSeq[Int],
      defaultNs: List[Int],
      namespaces: List[(String, List[Int])],
      classNs: String
  ): Array[String] = {

    val n = indices.size

    // Use a (mutable) array (and iteration) for speed.
    // The first row is the shared features. These are features that are not label dependent.
    // Then come the features for each of the n labels.
    val x = new Array[String](n + 1)

    val shared = VwRowCreator.unlabeledVwInput(features, defaultNs, namespaces, false)
    x(0) = SharedFeatureIndicator + shared

    var i = 0
    while (i < n) {
      val labelInd = indices(i)
      x(i + 1) = s"$labelInd:0 |$classNs _$labelInd"
      i += 1
    }

    x
  }

  /**
    * A producer that can produce a [[VwMultilabelRowCreator]].
    * The requirement for [[RowCreatorProducer]] to only have zero-argument constructors is
    * relaxed for this Producer because we don't have a way of generically constructing a
    * list of labels.  If the labels were encoded in the JSON, then a JsonReader for the label
    * type would have to be passed to the constructor.  Since the labels can't be encoded
    * generically in the JSON, we accept that this Producer is a special case and allow the labels
    * to be passed directly.  The consequence is that this producer doesn't just rely on the
    * dataset specification and the data itself.  It also relying on the labels provided to the
    * constructor.
    *
    * @param allLabelsInTrainingSet All of the labels that will be encountered in the training set.
    * @param ev$1 reflection information about `K`.
    * @tparam A type of input passed to the [[RowCreator]].
    * @tparam K the label type.
    */
  final class Producer[A, K: RefInfo](allLabelsInTrainingSet: sci.IndexedSeq[K])
      extends PositiveLabelsFunction[A, K]
         with RowCreatorProducer[A, Array[String], VwMultilabelRowCreator[A, K]]
         with RowCreatorProducerName
         with VwCovariateProducer[A]
         with DvProducer
         with SparseCovariateProducer
         with CompilerFailureMessages {

    override type JsonType = VwMultilabeledJson

    /**
      * Attempt to parse the JSON AST to an intermediate representation that is used
      * to create the row creator.
      * @param json JSON AST.
      * @return
      */
    override def parse(json: JsValue): Try[VwMultilabeledJson] =
      Try { json.convertTo[VwMultilabeledJson] }

    /**
      * Attempt to produce a Spec.
      *
      * @param semantics semantics used to make sense of the features in the JsonSpec
      * @param jsonSpec  a JSON specification to transform into a RowCreator.
      * @return
      */
    override def getRowCreator(
        semantics: CompiledSemantics[A],
        jsonSpec: VwMultilabeledJson
    ): Try[VwMultilabelRowCreator[A, K]] = {
      val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

      val rc = for {
        cov          <- covariates
        pos          <- positiveLabelsFn(semantics, jsonSpec.positiveLabels)
        labelNs      <- labelNamespaces(nss)
        actualLabelNs = labelNs.labelNs
        dummyLabelNs  = labelNs.dummyLabelNs
        sem           = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
      } yield new VwMultilabelRowCreator[A, K](allLabelsInTrainingSet, cov, default, nss,
                                               normalizer, pos, actualLabelNs, dummyLabelNs)

      rc
    }
  }

  private[aloha] final case class LabelNamespaces(labelNs: Char, dummyLabelNs: Char)
}
