package com.eharmony.aloha.models.vw.jni

import java.io._
import java.util
import java.{lang => jl}

import com.eharmony.aloha.dataset.json.SparseSpec.SparseSpecOps
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import com.eharmony.aloha.dataset.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.sources.{Base64StringSource, ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.reg.json.Spec
import com.eharmony.aloha.models.reg.{ConstantDeltaSpline, RegFeatureCompiler}
import com.eharmony.aloha.models.{BaseModel, TypeCoercion}
import com.eharmony.aloha.reflect._
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.{MorphableSemantics, Semantics}
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, Logging, SimpleTypeSeq}
import com.mwt.explorers.GenericExplorer
import com.mwt.scorers.Scorer
import deaktator.reflect.runtime.manifest.ManifestParser
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import spray.json.{DeserializationException, JsValue, JsonReader, pimpAny, pimpString}
import vowpalWabbit.learner._
import vowpalWabbit.responses.ActionProbs

import scala.Serializable
import scala.collection.immutable.{IndexedSeq, ListMap}
import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}

private[this] case object DefaultScorer extends Scorer[ActionProbs] {

  def scoreActions(context: ActionProbs): util.ArrayList[jl.Float] = {
    context.probsAsList
  }

}

abstract class VwJniModel[-A, +B](implicit private[this] val scB: ScoreConverter[B])
  extends BaseModel[A, B]
    with Logging
    with Serializable {

  type VwInput

  val vwParams: String
  val modelSource: ModelSource
  val featureNames: sci.IndexedSeq[String]
  val featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]]
  val defaultNs: List[Int]
  val namespaces: List[(String, List[Int])]
  val numMissingThreshold: Option[Int]

  val learnerCreator: VWLearner => VwEvaluator[A, VwInput, Either[(Seq[String], Seq[String]), (B, Option[Float])]]
  val vwInputGen: VwInputGenerator[A, VwInput]

  /**
    * This is the function responsible to scoring, using VW.
    */
  @transient private[jni] lazy val learnerEvaluator = learnerCreator(vwLearner)

  @transient private[jni] lazy val vwLearner: VWLearner = {
    val (learner, deletedFile) = VwJniModel.getVwLearner(modelSource, vwParams, modelId)
    deletedFile.foreach(_.delete())
    learner
  }

  {
    require(
      featureNames.size == featureFunctions.size,
      s"featureNames.size (${featureNames.size}}) != featureFunctions.size (${featureFunctions.size}})"
    )

    // Initialize the lazy vals.  This is done so that errors will be thrown on creation.
    require(vwLearner != null)
    require(learnerEvaluator != null)
  }

  /** Produce a score.
    * @param a an input to the model representing covariate data.
    * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
    * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
    *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
    *         sub-model scores.
    */
  override private[aloha] final def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
    vwInputGen(a) match {
      case Left(missing) => failure(Seq(s"Too many features with missing variables: ${missing.count(_._2.nonEmpty)}"), getMissingVariables(missing))
      case Right(vwIn) =>
        Try { learnerEvaluator(a, vwIn) } match {
          case Success(y) => y match {
            case Left(errorsMissing) => failure(errorsMissing._1, errorsMissing._2)
            case Right(x) => success(score = x._1, probability = x._2)
          }
          case Failure(ex) => failure(Seq(ex.getMessage))
        }
    }
  }

  /**
    * Close the underlying VW model.
    */
  override def close(): Unit = vwLearner.close()

  // TODO: Figure out how to make the missing features fast.
  private[jni] def getMissingVariables(missing: scm.Map[String, Seq[String]]): Seq[String] =
  missing.unzip._2.foldLeft(Set.empty[String])(_ ++ _).toIndexedSeq.sorted

}

object VwJniModel extends ParserProviderCompanion with VwJniModelJson with Logging {

  /**
   * A regular expression to check whether an initial regressor is present in the VW parameters.
   */
  private[jni] val InitialRegressorPresent = """^(.*\s)?-i.*$"""

  private[jni] val classLabelsKey = "classLabels"

  override def parser: ModelParser = Parser

  object Parser extends ModelParserWithSemantics
                   with EitherHelpers
                   with RegFeatureCompiler { self =>

    val modelType = "VwJNI"

    override def modelJsonReader[A, B](semantics: Semantics[A])(implicit jrB: JsonReader[B], scB: ScoreConverter[B]): JsonReader[VwJniModel[A, B]] = new JsonReader[VwJniModel[A, B]] {
      override def read(json: JsValue): VwJniModel[A, B] = {
        val vw = json.convertTo[VwJNIAst]

        features(vw.features.getOrElse(ListMap.empty).toSeq, semantics) match {
          case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
          case Right(featureMap) =>
            val (names, functions) = featureMap.toIndexedSeq.unzip
            val (defaultNs, nss) = getNamespaceFeatureIndexMap(featureMap, vw.namespaces, vw.labelDependentFeatures.getOrElse(ListMap.empty[String, Spec]).keySet)

            val vwParams = vw.vw.params.fold("")(_.fold(_.mkString(" "), x => x)).trim

            // We have to create these conversion functions and reference elements in the
            // vw object outside of the learnerCreator or else we have serialization issues.
            // Also we cannot create conversion functions for the array types because Aloha
            // doesn't handle array typed data yet.  As a result we'll just throw an example
            // at model instantiation if the user supplies a VW model which output an array type.

            val intCf = vw.classLabels.fold(getConversionFunction[Int, B])(l => getConversionFunction(l.values)(l.refInfo, scB))

            // doubleCf is only used in doubleCfSpline
            val doubleCf = getConversionFunction[Double, B]
            val doubleCfSpline = vw.spline.map(_.andThen(doubleCf)).getOrElse(doubleCf)

            val saltFunc = vw.salt.map(spec => semantics.createFunction[Long](spec).fold(l => throw new DeserializationException(l.mkString("\n")), identity))

            (vw.labelDomain, vw.labelType, vw.labelDependentFeatures, vw.scoreExtractor) match {
              case (Some(labelDomainSpec), Some(labelTypeString), Some(ldfs), Some(scoreExtractorSpec)) =>
                val labelType: RefInfo[_] = ManifestParser.parse(labelTypeString) match {
                  case Left(error) => throw new DeserializationException(s"Error while parsing labelType $labelTypeString: $error")
                  case Right(lt) => lt
                }
                val labelDomainFn = getLabelDomainFn(labelDomainSpec, semantics)(labelType)
                val ldfSemantics = semantics match {
                  case ms: MorphableSemantics[_, _] => ms.morph(labelType).getOrElse(
                    throw new ClassCastException(s"${ms.getClass.getCanonicalName} could not be morphed to $labelTypeString")
                  )
                  case ms: Semantics[_] => throw new IllegalStateException(s"${ms.getClass.getCanonicalName} is not a MorphableSemantics.")
                  case ms => throw new IllegalStateException(s"${ms.getClass.getCanonicalName} is not a Semantics.")
                }
                val scoreExtractorFn = getScoreExtractorFn(scoreExtractorSpec, ldfSemantics.asInstanceOf[Semantics[Any]])(scB.ri)
                features(ldfs.toSeq, ldfSemantics) match {
                  case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
                  case Right(labelDependentFeatureMap) =>
                    val (labelDependentFeatureNames, labelDependentFeatureFunctions: IndexedSeq[GenAggFunc[Any, Iterable[(String, Double)]]]) = labelDependentFeatureMap.toIndexedSeq.unzip
                    val (labelDependentFeatureDefaultNs, labelDependentFeatureNss) = getNamespaceFeatureIndexMap(labelDependentFeatureMap, vw.namespaces, names.toSet)
                    val learnerCreator = (vwLearner: VWLearner) => {
                      vwLearner match {
                        case l: VWMulticlassLearner => DefaultEvaluator((_: A, x: Array[String]) => Right((intCf(l.predict(x)), None)))
                        case l: VWScalarLearner => DefaultEvaluator((_: A, x: Array[String]) => Right((doubleCfSpline(l.predict(x)), None)))
                        case l: VWActionProbsLearner => ContextualEvaluator(saltFunc.get, (a: A, salt: Long, x: Array[String]) => {
                          val prediction = l.predict(x)
                          val explorer = new GenericExplorer(DefaultScorer, prediction.getActionProbs.length)
                          val decision = explorer.chooseAction(salt, prediction)
                          val action = decision.getAction
                          val probability = decision.getProbability
                          val labels = labelDomainFn(a)
                          getLabelDependentFeatureModelOutput(labels, action, scoreExtractorFn, Some(probability))
                        })
                        case l: VWActionScoresLearner => DefaultEvaluator((a: A, x: Array[String]) => {
                          val action = l.predict(x).getActionScores.apply(0).getAction
                          val labels = labelDomainFn(a)
                          getLabelDependentFeatureModelOutput(labels, action, scoreExtractorFn)
                        })
                        case l: VWLearner => throw new UnsupportedOperationException(s"Unsupported learner type ${l.getClass.getCanonicalName} produced by arguments: $vwParams")
                        case d => throw new IllegalStateException(s"${d.getClass.getCanonicalName} is not a VWLeaner.")
                      }
                    }
                    VwMultiFeatureSetJniModel(
                      vw.modelId,
                      vwParams,
                      vw.vw.modelSource,
                      names,
                      functions,
                      defaultNs,
                      nss,
                      learnerCreator,
                      vw.numMissingThreshold,
                      labelDomainFn,
                      labelDependentFeatureNames,
                      labelDependentFeatureFunctions,
                      labelDependentFeatureDefaultNs,
                      labelDependentFeatureNss,
                      vw.numMissingLDFThreshold
                    )
                }
              case (None, None, None, None) =>
                val learnerCreator = (vwLearner: VWLearner) => {
                  vwLearner match {
                    case l: VWMulticlassLearner => DefaultEvaluator((_: A, x: String) => Right((intCf(l.predict(x)), None)))
                    case l: VWScalarLearner => DefaultEvaluator((_: A, x: String) => Right((doubleCfSpline(l.predict(x)), None)))
                    case l: VWActionProbsLearner => ContextualEvaluator(saltFunc.get, (_: A, salt: Long, x: String) => {
                      val prediction = l.predict(x)
                      val explorer = new GenericExplorer(DefaultScorer, prediction.getActionProbs.length)
                      val decision = explorer.chooseAction(salt, prediction)
                      val action = decision.getAction
                      val probability = decision.getProbability
                      Right((intCf(action), Some(probability)))
                    })
                    case l: VWLearner => throw new UnsupportedOperationException(s"Unsupported learner type ${l.getClass.getCanonicalName} produced by arguments: $vwParams")
                    case d => throw new IllegalStateException(s"${d.getClass.getCanonicalName} is not a VWLeaner.")
                  }
                }
                VwSingleFeatureSetJniModel(
                  vw.modelId,
                  vwParams,
                  vw.vw.modelSource,
                  names,
                  functions,
                  defaultNs,
                  nss,
                  learnerCreator,
                  vw.numMissingThreshold
                )
              case (_, _, _, _) => throw new IllegalArgumentException(s"One or more, but not all, of labelDomainFn, labelType, labelDependentFeatures and scoreExtractor absent")
            }
        }
      }

    }
  }

  /**
   * Returns the model and the file, that when present should be deleted.
   * @param modelSource a source for the model
   * @param vwParams the "command" used to instantiate VW.
   * @return
   */
  private[jni] def getVwLearner(modelSource: ModelSource, vwParams: String, modelId: ModelIdentity): (VWLearner, Option[File]) = {
    val file = localModelFile(modelSource)
    val newParams = updatedVwModelParams(file, vwParams, modelId)
    val vwLearner: VWLearner = VWLearners.create(newParams)
    (vwLearner, if (modelSource.shouldDelete) Some(file) else None)
  }

  private[jni] def localModelFile(modelSource: ModelSource) = modelSource.localVfs.fileObj

  /**
   * Takes the params string and injects the initial regressor parameter.
   * @param file locations
   * @param vwParams the "command" used to instantiate VW.
   * @return
   */
  private[jni] def updatedVwModelParams(file: java.io.File, vwParams: String, modelId: ModelIdentity): String = {
    val initialRegressorParam = s" -i ${file.getCanonicalPath} "

    if (vwParams matches VwJniModel.InitialRegressorPresent)
      throw new IllegalArgumentException(s"For model $modelId, initial regressor (-i) vw parameter supplied and model provided.")

    initialRegressorParam + vwParams
  }

  def getConversionFunction[C : RefInfo, B : ScoreConverter](implicit riC: RefInfo[C], scB: ScoreConverter[B]) = {
    TypeCoercion[C, B](riC, scB.ri) getOrElse {
      throw new DeserializationException(s"Couldn't find conversion function to ${RefInfoOps.toString(scB.ri)}")
    }
  }

  def getConversionFunction[C : RefInfo, B : ScoreConverter](labels: Vector[C]): Int => B = {
    val cb = getConversionFunction[C, B]
    (i: Int) => cb(labels(i - 1))
  }

  private[jni] def readBinaryVwModelToB64String(in: InputStream, close: Boolean = true): String = {
    try {
      val bytes = IOUtils toByteArray in
      val encoded = Base64.encodeBase64(bytes)
      new String(encoded)
    }
    finally {
      if (close)
        IOUtils closeQuietly in
    }
  }

  private[jni] def getLabelDomainFn[A, LabelType: RefInfo](labelDomainSpec: String, semantics: Semantics[A]): GenAggFunc[A, Seq[LabelType]] = {
    semantics.createFunction[Seq[LabelType]](labelDomainSpec, None) match {
      case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
      case Right(labelDomainFn) => labelDomainFn
    }
  }

  private[jni] def getScoreExtractorFn[LabelType, B: RefInfo](scoreExtractorSpec: String, semantics: Semantics[LabelType]): GenAggFunc[LabelType, Option[B]] = {
    semantics.createFunction[Option[B]](scoreExtractorSpec, Some(None)) match {
      case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
      case Right(labelDomainFn) => labelDomainFn
    }
  }

  private[jni] def getNamespaceFeatureIndexMap[A](
                                                   featureMap: Seq[(String, GenAggFunc[A, Iterable[(String, Double)]])],
                                                   namespaces: Option[ListMap[String, Seq[String]]],
                                                   remainingFeatureNames: Set[String]
                                                 ): (List[Int], List[(String, List[Int])]) = {
    val indices = featureMap.unzip._1.view.zipWithIndex.toMap
    val nssRaw = namespaces.getOrElse(sci.ListMap.empty)
    val nss = nssRaw.map { case (ns, fs) =>
      val fi = fs.flatMap { f =>
        val oInd = indices.get(f)
        if (oInd.isEmpty && !remainingFeatureNames.contains(f)) info(s"Ignoring feature '$f' in namespace '$ns'.  Not in the feature list.")
        oInd
      }.toList
      (ns, fi)
    }.toList
    val defaultNs = (indices.keySet -- (Set.empty[String] /: nssRaw)(_ ++ _._2)).flatMap(indices.get).toList
    (defaultNs, nss)
  }

  private[jni] def getLabelDependentFeatureModelOutput[A, B](
                                                              labels: Seq[A],
                                                              labelIndex: Int,
                                                              scoreExtractor: GenAggFunc[A, Option[B]],
                                                              probability: Option[Float] = None
                                                            ): Either[(Seq[String], Seq[String]), (B, Option[Float])] = {
    val chosenLabelTry = Try { labels(labelIndex) }
    chosenLabelTry match {
      case Success(chosenLabel) =>
        val outputOpt = scoreExtractor(chosenLabel)
        outputOpt match {
          case Some(output) => Right((output, probability))
          case None =>
            val scoreExtractorProblems = scoreExtractor.accessorOutputProblems(chosenLabel)
            Left((scoreExtractorProblems.errors, scoreExtractorProblems.missing))
        }
      case Failure(ex) => Left((Seq(s"VWLearner returned label index $labelIndex for label sequence of length ${labels.size}"), Nil))
    }
  }

  /**
   *
   * @param spec
   * @param model
   * @param id
   * @param vwArgs
   * @param externalModel
   * @param numMissingThreshold
   * @param notes
   * @param spline
   * @return
   */
  private[eharmony] def json(spec: Vfs,
                             model: Vfs,
                             id: ModelId,
                             vwArgs: Option[String],
                             externalModel: Boolean = false,
                             numMissingThreshold: Option[Int] = None,
                             notes: Option[Seq[String]] = None,
                             spline: Option[ConstantDeltaSpline] = None): JsValue = {

    val js = StringReadable.fromInputStream(spec.inputStream).parseJson
    val vw = js.convertTo[VwUnlabeledJson]
    val classLabels = js.asJsObject.fields.get("classLabels").map(_.convertTo[SimpleTypeSeq])

    json(vw, model, id, vwArgs, externalModel, numMissingThreshold, notes, spline, classLabels)
  }

  /**
   *
   * @param vw
   * @param model
   * @param id
   * @param vwArgs
   * @param externalModel
   * @param numMissingThreshold
   * @param notes
   * @param spline
   * @return
   */
  private[eharmony] def json(vw: VwJsonLike,
                             model: Vfs,
                             id: ModelId,
                             vwArgs: Option[String],
                             externalModel: Boolean,
                             numMissingThreshold: Option[Int],
                             notes: Option[Seq[String]],
                             spline: Option[ConstantDeltaSpline],
                             classLabels: Option[SimpleTypeSeq]): JsValue = {

    def escape(s: String) = s.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"")

    val features = Some(ListMap(vw.features.map(f => f.name -> f.toModelSpec):_*))
    val ns = vw.namespaces.map(nss => ListMap(nss.map(n => n.name -> n.features):_*))

    // If this doesn't work, use commons-lang3 StringEscapeUtils.unescapeJava for unescaping.
    // Removed commons-lang3 as a dependency because it's only used in 2 places.  Here and
    // aloha-core CsvModelRunner class.  Here's how it was originally:
    //
    //   val vwParams = Option(vwArgs).filter(_.trim.nonEmpty).map(args => Right(StringEscapeUtils.escapeJson(args)))
    val vwParams = vwArgs.filter(_.trim.nonEmpty).map(args => Right(escape(args)))

    val params = vwArgs.getOrElse("").trim

    val vwObj = if (externalModel)
      Vw(ExternalSource(model), vwParams)
    else {
      val b64Model = VwJniModel.readBinaryVwModelToB64String(model.inputStream)
      Vw(Base64StringSource(b64Model), vwParams)
    }

    VwJNIAst(VwJniModel.parser.modelType, id, features, vwObj, ns, numMissingThreshold, notes, spline, classLabels).toJson
  }
}
