package com.eharmony.aloha.models.vw.jni

import java.io._

import com.eharmony.aloha.AlohaException
import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.dataset.SparseFeatureExtractorFunction
import com.eharmony.aloha.dataset.json.SparseSpec.SparseSpecOps
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.dataset.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.aloha.factory.{ModelParser, ModelSubmodelParsingPlugin, ParserProviderCompanion, SubmodelFactory}
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.sources.{Base64StringSource, ExternalSource, ModelSource}
import com.eharmony.aloha.io.vfs.Vfs
import com.eharmony.aloha.models.reg.{ConstantDeltaSpline, RegFeatureCompiler, RegressionFeatures, Spline}
import com.eharmony.aloha.models.{SubmodelBase, Subvalue, TypeCoercion}
import com.eharmony.aloha.reflect._
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, Logging, SimpleTypeSeq}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import spray.json.{DeserializationException, JsValue, JsonFormat, JsonReader, pimpAny, pimpString}
import vowpalWabbit.learner._

import scala.collection.immutable.ListMap
import scala.collection.{breakOut, immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}

/**
 * Model that delegates to a VW JNI model.
 *
 *
 *
 * @param modelId ModelIdentity
 * @param vwParams the "command" used to instantiate VW.
 * @param modelSource either the FsInstance pointing to the VW model or the Base64 encoded VW model.
 * @param featureNames names of features (parallel to featureFunctions)
 * @param featureFunctions functions to extract values from the input value
 * @param defaultNs indices of features that will be placed in the default VW namespace
 * @param namespaces mapping from namespace name to indices of features that will be placed in the namespace
 * @param learnerCreator A function that when given a VWLearner creates a function that can make a prediction, given
 *                       string-based input.
 * @param numMissingThreshold A threshold dictating how many missing features to allow before making the
 *                            prediction fail.  See ''com.eharmony.aloha.models.reg.RegressionFeatures.numMissingThreshold''
 *                            in aloha-core.
 * @tparam A model input type
 * @tparam B model output type
 * @author R M Deak
 */
final case class VwJniModel[U, N, -A, +B <: U](
    modelId: ModelIdentity,
    vwParams: String,
    modelSource: ModelSource,
    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    learnerCreator: VWLearner => String => N,
    auditor: Auditor[U, N, B],
    numMissingThreshold: Option[Int] = None)
  extends SubmodelBase[U, N, A, B]
     with RegressionFeatures[A]
     with Logging {

  /**
   * The object responsible to taking the computed features and turning it into a VW input formatted string.
   * This is a transient lazy val so that we can properly serialize
   */
  private[this] val vwRowCreator =
    new VwRowCreator(SparseFeatureExtractorFunction(featureNames zip featureFunctions), defaultNs, namespaces, None)

  @transient private[jni] lazy val vwLearner: VWLearner = {
    val (learner, deletedFile) = VwJniModel.getVwLearner(modelSource, vwParams, modelId)
    deletedFile.foreach(_.delete())
    learner
  }

  /**
   * This is the function responsible to scoring, using VW.
   */
  @transient private[this] lazy val learnerEvaluator: String => N = learnerCreator(vwLearner)

  {
    require(
      featureNames.size == featureFunctions.size,
      s"featureNames.size (${featureNames.size}}) != featureFunctions.size (${featureFunctions.size}})")

    val req = sci.BitSet(featureFunctions.indices:_*)
    val act = (sci.BitSet(defaultNs:_*) /: namespaces)(_ ++ _._2)
    require(
      req == act,
      s"defaultNamespace and namespaces must cover all indices (0 until ${featureFunctions.size}).  Missing indices: ${(req -- act).map(i => s"$i='${featureNames(i)}'").mkString(", ")}.")

    // Initialize the lazy vals.  This is done so that errors will be thrown on creation.
    require(vwRowCreator != null)
    require(vwLearner != null)
    require(learnerEvaluator != null)
  }

  override def subvalue(a: A): Subvalue[B, N] = {
    generateVwInput(a) match {
      case (None, missing) =>
        failure(
          Seq(s"Too many features with missing variables: ${missing.count(_._2.nonEmpty)}"),
          getMissingVariables(missing))
      case (Some(vwIn), missing) =>
        Try { learnerEvaluator(vwIn) } match {
          case Success(y) => success(y, missingVarNames = getMissingVariables(missing))
          case Failure(ex) => failure(Seq(ex.getMessage), getMissingVariables(missing))
        }
    }
  }


  /**
   * Close the underlying VW model.
   */
  override def close(): Unit = vwLearner.close()


  // TODO: Figure out how to make the missing features fast.
  private[jni] def getMissingVariables(missing: scm.Map[String, Seq[String]]): Set[String] =
    missing.flatMap(_._2)(breakOut)

  /**
   * Get a VW line (on the right) or a map of missing features (on the left) if there was too much
   * missing data to form a prediction.
   * @param a input
   * @return a result
   */
  private[jni] def generateVwInput(a: A) = {
    val f = constructFeatures(a)
    if (f.missingOk) (Option(vwRowCreator.unlabeledVwInput(f.features).toString), f.missing)
    else             (None, f.missing)
  }
}

object VwJniModel
 extends ParserProviderCompanion
    with VwJniModelJson
    with Logging {

  private[jni] def toLabelFn[N: RefInfo](labels: Option[SimpleTypeSeq]): Int => N = {
    labels map { ls =>
      val tc = convFn(ls.refInfo, RefInfo[N])

      // VW uses 1-based indexing for classes.  0-based indexing is better for dereference arrays of values, etc.
      (i: Int) => tc(ls.values(i - 1))
    } getOrElse {
      convFn[Int, N]
    }
  }

  private[jni] def convFn[A: RefInfo, B: RefInfo] = {
    TypeCoercion[A, B] getOrElse {
      throw new AlohaException(s"Couldn't find a conversion to from ${RefInfoOps.toString[A]} to ${RefInfoOps.toString[B]}.")
    }
  }

  // Note the repetitiveness below.  This is because the underlying base class is package private.  This is
  // good so that the class cannot be extended outside the vowpalWabbit.learner package, but it leads to
  // repetitive code.  Perhaps there should be a separate public interface apart from the abstract base class.

  // TODO: This is where additional VW learners should be wrapped.

  private[jni] def multiclassLearner[N: RefInfo](l: VWMulticlassLearner, labels: Option[SimpleTypeSeq]) = {
    val toLabel = toLabelFn[N](labels)
    (s: String) => toLabel(l.predict(s))
  }

  private[jni] def scalarLearner[N: RefInfo](l: VWScalarLearner, spline: Option[Spline]) = {
    val doubleCf = convFn[Double, N]
    val doubleCfSpline = spline.map(_.andThen(doubleCf)).getOrElse(doubleCf)
    (x: String) => doubleCfSpline(l.predict(x))
  }

  private[jni] def probLearner[N: RefInfo](l: VWProbLearner, spline: Option[Spline]) = {
    val doubleCf = convFn[Double, N]
    val doubleCfSpline = spline.map(_.andThen(doubleCf)).getOrElse(doubleCf)
    (x: String) => doubleCfSpline(l.predict(x))
  }

  /**
    * LearnerCreator is designed to avoid gotchas that lead to serialization exceptions.  This class avoids
    * creating the type coercion function until the apply function is called.  Additionally, the binary VW
    * model isn't turned into an actual VWLearner here.  This class is designed to take a VWLearner that is
    * deserialized and adapt it so that it works with Aloha.
    *
    *
    * @param labels labels for contextual bandits
    * @param spline an optional spline for regression models
    * @param vwParams the command line parameters with which VW is to be invoked.
    * @param rin reflection information necessary to create the type coercion
    * @tparam N the natural type of the VwJniModel instance.
    */
  private[this] case class LearnerCreator[N](labels: Option[SimpleTypeSeq], spline: Option[Spline], vwParams: String)
                                            (implicit rin: RefInfo[N]) extends (VWLearner => String => N) {
    override def apply(vwLearner: VWLearner): String => N = vwLearner match {
      case m: VWMulticlassLearner => multiclassLearner(m, labels)
      case p: VWProbLearner       => probLearner(p, spline)
      case s: VWScalarLearner     => scalarLearner(s, spline)
      case l: VWLearner           =>
        throw new UnsupportedOperationException(s"Unsupported learner type ${l.getClass.getCanonicalName} produced by arguments: $vwParams")
      case d => throw new IllegalStateException(s"${d.getClass.getCanonicalName} is not a VWLearner.")
    }
  }

  /**
   * A regular expression to check whether an initial regressor is present in the VW parameters.
   */
  private[jni] val InitialRegressorPresent = """^(.*\s)?-i.*$"""

  private[jni] val classLabelsKey = "classLabels"

  override def parser: ModelParser = Parser

  object Parser extends ModelSubmodelParsingPlugin
                   with EitherHelpers
                   with RegFeatureCompiler
                   with Namespaces { self =>

    val modelType = "VwJNI"

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[VwJniModel[U, N, A, B]]] = {

      Some(new JsonReader[VwJniModel[U, N, A, B]] {
        override def read(json: JsValue): VwJniModel[U, N, A, B] = {

          val vw = json.convertTo[VwJNIAst]

          features(vw.features.toSeq, semantics) match {
            case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
            case Right(featureMap) =>
              val (names, functions) = featureMap.toIndexedSeq.unzip

              val (nss, defaultNs, missing) =
                allNamespaceIndices(names, vw.namespaces.getOrElse(sci.ListMap.empty))

              if (missing.nonEmpty)
                info(s"Ignoring features in namespaces not in feature list: $missing")

              val vwParams = vw.vw.params.fold("")(_.fold(_.mkString(" "), x => x)).trim
              val learnerCreator = LearnerCreator[N](vw.classLabels, vw.spline, vwParams)

              VwJniModel(
                vw.modelId,
                vwParams,
                vw.vw.modelSource,
                names,
                functions,
                defaultNs,
                nss,
                learnerCreator,
                auditor,
                vw.numMissingThreshold)
          }
        }
      })
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
    val vwLearner = VWLearners.create[VWLearner](newParams)
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
  def json(spec: Vfs,
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
  def json(vw: VwJsonLike,
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

    val vwObj = if (externalModel)
      Vw(ExternalSource(model), vwParams)
    else {
      val b64Model = VwJniModel.readBinaryVwModelToB64String(model.inputStream)
      Vw(Base64StringSource(b64Model), vwParams)
    }

    VwJNIAst(VwJniModel.parser.modelType, id, features, vwObj, ns, numMissingThreshold, notes, spline, classLabels).toJson
  }
}
