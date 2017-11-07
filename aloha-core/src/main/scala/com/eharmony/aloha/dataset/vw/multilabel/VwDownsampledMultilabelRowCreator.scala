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
  * Created by ryan.deak on 11/6/17.
  *
  * @param allLabelsInTrainingSet
  * @param featuresFunction
  * @param defaultNamespace
  * @param namespaces
  * @param normalizer
  * @param positiveLabelsFunction
  * @param classNs
  * @param dummyClassNs
  * @param numDownsampledLabels
  * @param initialSeed a way to start off randomness
  * @param includeZeroValues include zero values in VW input?
  * @tparam A
  * @tparam K
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
    numDownsampledLabels: Int,
    initialSeed: () => Long,
    includeZeroValues: Boolean = false
) extends StatefulRowCreator[A, Array[String], Long]
     with Logging {

  import VwDownsampledMultilabelRowCreator._

  @transient private[this] lazy val labelToInd = allLabelsInTrainingSet.zipWithIndex.toMap

  private[this] val negativeDummyStr =
    s"$NegDummyClassId:$NegativeCost |$dummyClassNs $NegativeDummyClassFeature"

  private[this] val positiveDummyStr =
    s"$PosDummyClassId:$PositiveCost |$dummyClassNs $PositiveDummyClassFeature"

  /**
    * Some initial state that can be used on the very first call to `apply(A, S)`.
    *
    * @return some state.
    */
  @transient override lazy val initialState: Long = {

    val seed = initialSeed()

    // For logging.  Try to get time as close as possible to calling initialSeed.
    // Note: There's a good chance this will differ.
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
    * Given an `a` and some `state`, produce output, including a new state.
    *
    * When using this function, the user is responsible for keeping track of,
    * and providing the state.
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

    // TODO: Should this be sci.BitSet?
    val positiveIndices: Set[Int] =
      positiveLabelsFunction(a).flatMap { y => labelToInd.get(y).toSeq }(breakOut)

    val (x, newSeed) = sampledTrainingInput(
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
      numDownsampledLabels
    )

    ((missingAndErrs, Option(x)), newSeed)
  }
}


object VwDownsampledMultilabelRowCreator extends Rand {

  private def scramble(initSeed: Long): Long = initSeedScramble(initSeed)

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
      if (negLabels <= numNegLabelsTarget) {
        // No downsampling occurs.
        NegativeCost.toString
      }
      else {
        // Determine the weight for the downsampled negatives.
        // If the cost for positive examples is 0, and negative examples have cost 1,
        // the weight will be in the interval (NegativeCost, Infinity).

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
