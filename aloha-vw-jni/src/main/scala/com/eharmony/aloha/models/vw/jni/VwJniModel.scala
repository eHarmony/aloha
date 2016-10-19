package com.eharmony.aloha.models.vw.jni

import java.io._
import java.lang.Float
import java.util

import com.eharmony.aloha.dataset.SparseFeatureExtractorFunction
import com.eharmony.aloha.dataset.json.SparseSpec.SparseSpecOps
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.dataset.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.sources.{Base64StringSource, ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.{Vfs, VfsType}
import com.eharmony.aloha.models.reg.{ConstantDeltaSpline, RegFeatureCompiler, RegressionFeatures}
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

import scala.collection.immutable.{IndexedSeq, ListMap}
import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}

private[this] case object DefaultScorer extends Scorer[ActionProbs] {

  override def scoreActions(context: ActionProbs): util.ArrayList[Float] = {
    context.probsAsList
  }

}

trait VwJniModel[-A, +B] extends BaseModel[A, B]
  with RegressionFeatures[A]
  with Logging {

  val modelId: ModelIdentity
  val vwParams: String
  val modelSource: ModelSource
  val featureNames: sci.IndexedSeq[String]
  val featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]]
  val defaultNs: List[Int]
  val namespaces: List[(String, List[Int])]
  val numMissingThreshold: Option[Int]

  /**
    * The object responsible to taking the computed features and turning it into a VW input formatted string.
    * This is a transient lazy val so that we can properly serialize
    */
  private[jni] val vwRowCreator =
  new VwRowCreator(SparseFeatureExtractorFunction(featureNames zip featureFunctions), defaultNs, namespaces, None)

  @transient private[jni] lazy val vwLearner: VWLearner = {
    val (learner, deletedFile) = VwJniModel.getVwLearner(modelSource, vwParams, modelId)
    deletedFile.foreach(_.delete())
    learner
  }

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

        features(vw.features.toSeq, semantics) match {
          case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
          case Right(featureMap) =>
            val (names, functions) = featureMap.toIndexedSeq.unzip

            val indices = featureMap.unzip._1.view.zipWithIndex.toMap
            val nssRaw = vw.namespaces.getOrElse(sci.ListMap.empty)
            val nss = nssRaw.map { case (ns, fs) =>
              val fi = fs.flatMap { f =>
                val oInd = indices.get(f)
                if (oInd.isEmpty) info(s"Ignoring feature '$f' in namespace '$ns'.  Not in the feature list.")
                oInd
              }.toList
              (ns, fi)
            }.toList

            val vwParams = vw.vw.params.fold("")(_.fold(_.mkString(" "), x => x)).trim

            val defaultNs = (indices.keySet -- (Set.empty[String] /: nssRaw)(_ ++ _._2)).flatMap(indices.get).toList

            // We have to create these conversion functions and reference elements in the
            // vw object outside of the learnerCreator or else we have serialization issues.
            // Also we cannot create conversion functions for the array types because Aloha
            // doesn't handle array typed data yet.  As a result we'll just throw an example
            // at model instantiation if the user supplies a VW model which output an array type.

            val intCf = vw.classLabels.fold(getConversionFunction[Int, B])(l => getConversionFunction(l.values)(l.refInfo, scB))
            val doubleCf = getConversionFunction[Double, B]
            val doubleCfSpline = vw.spline.map(_.andThen(doubleCf)).getOrElse(doubleCf)

            val saltFunc = vw.salt.map(spec => semantics.createFunction[Long](spec).fold(l => throw new DeserializationException(l.mkString("\n")), identity))

            (vw.labelDomain, vw.labelType, vw.ldfs) match {
              case (Some(labelDomainSpec), Some(labelTypeString), Some(ldfs)) =>
                // create new semantics, then basically rewrite the code from above starting at features(vw.features.toSeq, semantics) match {
                val labelType: RefInfo[_] = ManifestParser.parse(labelTypeString) match {
                  case Left(error) => throw new DeserializationException(error)
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
                features(ldfs.toSeq, ldfSemantics) match {
                  case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
                  case Right(ldfMap) =>
                    val (ldfNames, ldfFunctions) = ldfMap.toIndexedSeq.unzip
                    ???
                }
              case (None, None, None) =>
                val learnerCreator = (vwLearner: VWLearner) => {
                  vwLearner match {
                    case l: VWMulticlassLearner => Left((x: String) => intCf(l.predict(x)))
                    case l: VWScalarLearner => Left((x: String) => doubleCfSpline(l.predict(x)))
                    case l: VWActionProbsLearner => Right((x: String, salt: Long) => {
                      val prediction = l.predict(x)
                      val explorer = new GenericExplorer(DefaultScorer, prediction.getActionProbs.length)
                      val decision = explorer.chooseAction(salt, prediction)
                      val action = decision.getAction
                      val probability = decision.getProbability
                      // record probability
                      intCf(action)
                    })
                    case l: VWActionScoresLearner => Left((x: String) => intCf(l.predict(x).getActionScores.apply(0).getAction))
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
                  vw.numMissingThreshold,
                  saltFunc
                )
              case (_, _, _) => throw new IllegalArgumentException(s"One or more of, but not all, labelDomainFn, labelType, ldfs absent")
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

  private[jni] def getLabelDomainFn[A, LabelType: RefInfo](labelDomainSpec: String, semantics: Semantics[A]): GenAggFunc[A, sci.IndexedSeq[LabelType]] = {
    semantics.createFunction[sci.IndexedSeq[LabelType]](labelDomainSpec, None) match {
      case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
      case Right(labelDomainFn) => labelDomainFn
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

    val features = ListMap(vw.features.map(f => f.name -> f.toModelSpec):_*)
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
