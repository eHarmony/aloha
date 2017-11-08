package com.eharmony.aloha.dataset.vw.multilabel

import com.eharmony.aloha.dataset._
import com.eharmony.aloha.dataset.density.Sparse
import com.eharmony.aloha.dataset.vw.VwCovariateProducer
import com.eharmony.aloha.dataset.vw.multilabel.VwMultilabelRowCreator._
import com.eharmony.aloha.dataset.vw.multilabel.json.VwDownsampledMultilabeledJson
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.Logging
import com.eharmony.aloha.util.rand.Rand
import spray.json.JsValue

import scala.collection.{breakOut, immutable => sci}
import scala.util.Try


/**
  * Creates training data for multilabel models in Vowpal Wabbit's CSOAA LDF and WAP LDF format
  * for the JNI.  In this row creator, negative labels are downsampled and costs for the
  * downsampled labels are adjusted to produced an unbiased estimator.  It is assumed that
  * negative labels are in the majority. Downsampling negatives can improve both training
  * time and possibly model performance.  See the following resources for intuition:
  *
  - [[https://www3.nd.edu/~nchawla/papers/SPRINGER05.pdf Chawla, Nitesh V. "Data mining for
      imbalanced datasets: An overview." Data mining and knowledge discovery handbook.
      Springer US, 2009. 875-886.]]
  - [[https://www3.nd.edu/~dial/publications/chawla2004editorial.pdf Chawla, Nitesh V., Nathalie
      Japkowicz, and Aleksander Kotcz. "Special issue on learning from imbalanced data sets."
      ACM SIGKDD Explorations Newsletter 6.1 (2004): 1-6.]]
  - [[http://www.marcoaltini.com/blog/dealing-with-imbalanced-data-undersampling-oversampling-and-proper-cross-validation
      Dealing with imbalanced data: undersampling, oversampling, and proper cross validation,
      Marco Altini, Aug 17, 2015.]]
  *
  * This row creator, since it is stateful, requires the caller to maintain state.  If however,
  * it is only called via an iterator or sequence, then this row creator can maintain the state
  * during iteration over the iterator or sequence.  In the case of iterators, the mapping is
  * '''non-strict''' and in the case of sequences (`Seq`), it is '''strict'''.
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
  * @param numDownsampledNegLabels '''a positive value''' representing the number of negative
  *                                labels to include in each row.  If this is less than the
  *                                number of negative examples for a given row, then no
  *                                downsampling of negatives will take place.
  * @param seedCreator a "''function''" that creates a seed that will be used for randomness.
  *                    The implementation of this function is important.  It should create a
  *                    unique value for each unit of parallelism.  If for example, row
  *                    creation is parallelized across multiple threads on one machine, the
  *                    unit of parallelism is threads and `seedCreator` should produce unique
  *                    values for each thread.  If row creation is parallelized across multiple
  *                    machines, the `seedCreator` should produce a unique value for each
  *                    machine.  If row creation is parallelized across machines and threads on
  *                    each machine, the `seedCreator` should create unique values for each
  *                    thread on each machine.  Otherwise, randomness will be striped which

  *
  * @param includeZeroValues include zero values in VW input?
  * @tparam A the input type
  * @tparam K the label or class type
  * @author deaktator
  * @since 11/6/2017
  */
final case class VwDownsampledMultilabelRowCreator[-A, K](
    allLabelsInTrainingSet: sci.IndexedSeq[K],
    featuresFunction: FeatureExtractorFunction[A, Sparse],
    defaultNamespace: List[Int],
    namespaces: List[(String, List[Int])],
    normalizer: Option[CharSequence => CharSequence],
    positiveLabelsFunction: GenAggFunc[A, sci.IndexedSeq[K]],
    classNs: Char,
    dummyClassNs: Char,
    numDownsampledNegLabels: Int,
    seedCreator: () => Long,
    includeZeroValues: Boolean = false
) extends StatefulRowCreator[A, Array[String], Long]
     with Logging {

  require(
    0 < numDownsampledNegLabels,
    s"numDownsampledNegLabels must be positive, found $numDownsampledNegLabels"
  )

  import VwDownsampledMultilabelRowCreator._

  @transient private[this] lazy val labelToInd = allLabelsInTrainingSet.zipWithIndex.toMap

  // Precomputed for efficiency.

  private[this] val negativeDummyStr =
    s"$NegDummyClassId:$NegativeCost |$dummyClassNs $NegativeDummyClassFeature"

  private[this] val positiveDummyStr =
    s"$PosDummyClassId:$PositiveCost |$dummyClassNs $PositiveDummyClassFeature"

  /**
    * Some initial state that can be used on the very first call to `apply(A, S)`.
    * @return some state.
    */
  @transient override lazy val initialState: Long = {

    val seed = seedCreator()

    // For logging.  Try to get time as close as possible to calling initialSeed.
    // Note: There's a very good chance this will differ.
    val time       = System.nanoTime()
    val ip         = java.net.InetAddress.getLocalHost.getHostAddress
    val thread     = Thread.currentThread()
    val threadId   = thread.getId
    val threadName = thread.getName

    val scrambled  = scramble(seed)

    info(
      s"${getClass.getSimpleName} seed: $seed, scrambled: $scrambled, " +
      s"nanotime: $time, ip: $ip, threadId: $threadId, threadName: $threadName"
    )

    scrambled
  }

  /**
    * Given an `a` and some `seed`, produce output, including a new seed.
    *
    * When using this function, the user is responsible for keeping track of,
    * and providing the seeds.
    *
    * The implementation of this function should be referentially transparent.
    *
    * @param a     input
    * @param seed the random seed which is updated on each call.
    * @return a tuple where the first element is a Tuple2 whose first element is
    *         missing and error information and second element is an optional result.
    *         The second element of the outer Tuple2 is the new state.
    */
  override def apply(a: A, seed: Long): ((MissingAndErroneousFeatureInfo, Option[Array[String]]), Long) = {
    val (missingAndErrs, features) = featuresFunction(a)

    // Get the lazy val once.
    val labToInd = labelToInd

    // TODO: This seems like it could be optimized.
    // The positiveLabelsFunction is invoked once and all labels are produced.
    // Then a set is produced that is used to partition all of the labels in
    // the training data into positive and negative.  It seems like a slightly
    // more efficient way to do this would be to create a sorted array of positives
    // indices.  Then a negative index array could be constructed with the appropriate
    // size based on the size of allLabelsInTrainingSet and the size of the positives
    // array.  Next allLabelsInTrainingSet.indices and the positiveIndices array could
    // be iterated over simultaneously and if the current index in
    // allLabelsInTrainingSet.indices isn't in the positiveIndices array, then it must
    // be in the negativeIndices array.  Then then offsets into the two arrays are
    // incremented.  Both the current and proposed algorithms are O(N), where
    // N = allLabelsInTrainingSet.indices.size.  But the proposed algorithm would likely
    // have much better constant factors.
    //
    // Note that labels produced by positiveLabelsFunction that are not in the
    // allLabelsInTrainingSet are discarded without notice.
    //
    // TODO: Should this be sci.BitSet?
    val positiveIndices: Set[Int] = positiveLabelsFunction(a).flatMap(labToInd.get)(breakOut)

    val (vwInput, newSeed) = sampledTrainingInput(
      features,
      allLabelsInTrainingSet.indices,
      positiveIndices,
      defaultNamespace,
      namespaces,
      classNs,
      dummyClassNs,
      negativeDummyStr,
      positiveDummyStr,
      seed,
      numDownsampledNegLabels
    )

    ((missingAndErrs, Option(vwInput)), newSeed)
  }
}


object VwDownsampledMultilabelRowCreator extends Rand {

  // Expose initSeedScramble to companion class.
  private def scramble(initSeed: Long): Long = initSeedScramble(initSeed)

  /**
    *
    * '''This should not be used directly.''' ''It is exposed for testing.''
    *
    * @param features the common features produced
    * @param indices indices of all labels in the training set.
    * @param positiveLabelIndices indices of labels in the training set that positive are
    *                             positive for this training example
    * @param defaultNs list of feature indices in the default VW namespace.
    * @param namespaces a mapping from VW namespace name to feature indices in that namespace.
    * @param classNs the namespace name for class information.
    * @param dummyClassNs the namespace name for dummy class information.  2 dummy classes are
    *                     added to make the predicted probabilities work.
    * @param negativeDummyStr line in VW input associated with the negative dummy class.
    * @param positiveDummyStr line in VW input associated with the positive dummy class.
    * @param seed a seed for randomness.  The second part of the output of this function is a
    *             new seed that should be used on the next call to this function.
    * @param numNegLabelsTarget the desired number of negative labels.  If it is determined
    *                           that there are less negative labels than desired, no negative
    *                           label downsampling will occur; otherwise, the negative labels
    *                           will be downsampled to this target value.
    * @return an array representing the VW input that can either be passed directly to the
    *         VW JNI library, or `mkString("", "\n", "\n")` can be called to pass to the VW
    *         CLI.  The second part of the return value is a new seed to use as the `seed`
    *         parameter in the next call to this function.
    */
  private[multilabel] def sampledTrainingInput(
      features: IndexedSeq[Sparse],
      indices: sci.IndexedSeq[Int],
      positiveLabelIndices: Int => Boolean,
      defaultNs: List[Int],
      namespaces: List[(String, List[Int])],
      classNs: Char,
      dummyClassNs: Char,
      negativeDummyStr: String,
      positiveDummyStr: String,
      seed: Long,
      numNegLabelsTarget: Int
  ): (Array[String], Long) = {

    // Partition into positive and negative indices.
    val (posLabelInd, negLabelInd) = indices.partition(positiveLabelIndices)
    val negLabels = negLabelInd.size

    val numDownsampledLabels = math.min(numNegLabelsTarget, negLabels)

    // Sample indices in negLabelInd to use in the output.
    val (downsampledNegLabelInd, newSeed) =
      sampleCombination(negLabels, numDownsampledLabels, seed)

    val p = posLabelInd.size
    val n = downsampledNegLabelInd.length

    // This code is written with knowledge of the constant's value.
    // TODO: Write calling code to abstract over costs so knowledge of the costs isn't necessary.
    val negWt =
      if (numDownsampledLabels == negLabels) {
        // No downsampling occurs.
        NegativeCost.toString
      }
      else {
        // Determine the weight for the downsampled negatives.
        // If the cost of negative examples is positive, then the weight will be
        // strictly greater than .

        f"${NegativeCost * negLabels / n.toDouble}%.5g"
      }

    // The length of the output array is n + 3.
    //
    // The first row is the shared features. These are features that are not label dependent.
    // Then comes two dummy classes.  These are to make the probabilities work out.
    // Then come the features for each of the n labels.
    val x = new Array[String](p + n + 3)

    val shared = VwRowCreator.unlabeledVwInput(
      features, defaultNs, namespaces, includeZeroValues = false
    )

    x(0) = SharedFeatureIndicator + shared
    x(1) = negativeDummyStr
    x(2) = positiveDummyStr

    // vvvvv  This is mutable because we want speed.  vvvvv

    // Negative weights
    var ni = 0
    while (ni < n) {
      val labelInd = negLabelInd(downsampledNegLabelInd(ni))
      x(ni + 3) = s"$labelInd:$negWt |$classNs _$labelInd"
      ni += 1
    }

    // Positive weights
    var pi = 0
    while (pi < p) {
      val labelInd = posLabelInd(pi)
      x(pi + n + 3) = s"$labelInd:$PositiveCost |$classNs _$labelInd"
      pi += 1
    }

    (x, newSeed)
  }

  /**
    * A producer that can produce a [[VwDownsampledMultilabelRowCreator]].
    * The requirement for [[StatefulRowCreatorProducer]] to only have zero-argument constructors
    * is relaxed for this Producer because we don't have a way of generically constructing a
    * list of labels.  If the labels were encoded in the JSON, then a JsonReader for the label
    * type would have to be passed to the constructor.  Since the labels can't be encoded
    * generically in the JSON, we accept that this Producer is a special case and allow the labels
    * to be passed directly.  The consequence is that this producer doesn't just rely on the
    * dataset specification and the data itself.  It also relying on the labels provided to the
    * constructor.
    *
    * @param allLabelsInTrainingSet All of the labels that will be encountered in the training set.
    * @param seedCreator a "''function''" that creates a seed that will be used for randomness.
    *                    The implementation of this function is important.  It should create a
    *                    unique value for each unit of parallelism.  If for example, row
    *                    creation is parallelized across multiple threads on one machine, the
    *                    unit of parallelism is threads and `seedCreator` should produce unique
    *                    values for each thread.  If row creation is parallelized across multiple
    *                    machines, the `seedCreator` should produce a unique value for each
    *                    machine.  If row creation is parallelized across machines and threads on
    *                    each machine, the `seedCreator` should create unique values for each
    *                    thread on each machine.  Otherwise, randomness will be striped which
    * @param ev$1 reflection information about `K`.
    * @tparam A type of input passed to the [[StatefulRowCreator]].
    * @tparam K the label type.
    */
  final class Producer[A, K: RefInfo](
      allLabelsInTrainingSet: sci.IndexedSeq[K],
      seedCreator: () => Long
  )   extends PositiveLabelsFunction[A, K]
         with StatefulRowCreatorProducer[A, Array[String], Long, VwDownsampledMultilabelRowCreator[A, K]]
         with RowCreatorProducerName
         with VwCovariateProducer[A]
         with DvProducer
         with SparseCovariateProducer
         with CompilerFailureMessages {

    override type JsonType = VwDownsampledMultilabeledJson

    /**
      * Attempt to parse the JSON AST to an intermediate representation that is used
      * to create the row creator.
      * @param json JSON AST.
      * @return
      */
    override def parse(json: JsValue): Try[VwDownsampledMultilabeledJson] =
      Try { json.convertTo[VwDownsampledMultilabeledJson] }

    /**
      * Attempt to produce a Spec.
      *
      * @param semantics semantics used to make sense of the features in the JsonSpec
      * @param jsonSpec  a JSON specification to transform into a RowCreator.
      * @return
      */
    override def getRowCreator(
        semantics: CompiledSemantics[A],
        jsonSpec: VwDownsampledMultilabeledJson
    ): Try[VwDownsampledMultilabelRowCreator[A, K]] = {
      val (covariates, default, nss, normalizer) = getVwData(semantics, jsonSpec)

      val rc = for {
        cov          <- covariates
        pos          <- positiveLabelsFn(semantics, jsonSpec.positiveLabels)
        labelNs      <- labelNamespaces(nss)
        actualLabelNs = labelNs.labelNs
        dummyLabelNs  = labelNs.dummyLabelNs
        sem           = addStringImplicitsToSemantics(semantics, jsonSpec.imports)
        numNeg        = jsonSpec.numDownsampledNegLabels
      } yield new VwDownsampledMultilabelRowCreator[A, K](
          allLabelsInTrainingSet, cov, default, nss, normalizer,
          pos, actualLabelNs, dummyLabelNs, numNeg, seedCreator)

      rc
    }
  }
}
