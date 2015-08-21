package com.eharmony.aloha.models.vw.jni

import java.io._

import com.eharmony.aloha.dataset.SparseFeatureExtractorFunction
import com.eharmony.aloha.dataset.json.SparseSpec.SparseSpecOps
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.dataset.vw.unlabeled.json.VwUnlabeledJson
import com.eharmony.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.io.StringReadable
import com.eharmony.aloha.io.fs.FsType.FsType
import com.eharmony.aloha.io.fs._
import com.eharmony.aloha.models.reg.json.Spec
import com.eharmony.aloha.models.reg.{ConstantDeltaSpline, RegressionFeatures, Spline}
import com.eharmony.aloha.models.{BaseModel, TypeCoercion}
import com.eharmony.aloha.reflect._
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.basic.ModelOutput
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.GenAggFunc
import com.eharmony.aloha.util.{EitherHelpers, Logging}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import spray.json.{DeserializationException, JsValue, JsonReader, pimpAny, pimpString}
import vw.VW

import scala.collection.immutable.ListMap
import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}



/**
 * Model that delegates to a VW JNI model.
 * @param modelId a modelId
 * @param vwModel either the FsInstance pointing to the VW model or the Base64 encoded VW model.
 * @param vwParams VW parameters.  These are the same as the ones that would be passed on the command line,
 *                 directly to VW.
 * @param featureNames names of features (parallel to featureFunctions)
 * @param featureFunctions functions to extract values from the input value
 * @param defaultNs indices of features that will be placed in the default VW namespace
 * @param namespaces mapping from namespace name to indices of features that will be placed in the namespace
 * @param finalizer a function that transforms the native VW output type (Float) to B
 * @param numMissingThreshold A threshold dictating how many missing features to allow before making the
 *                            prediction fail.  See ''com.eharmony.aloha.models.reg.RegressionFeatures.numMissingThreshold''
 *                            in aloha-core.
 * @param scb a score converter
 * @tparam A model input type
 * @tparam B model output type
 * @author R M Deak
 */
final case class VwJniModel[-A, +B](
    modelId: ModelIdentity,
    vwModel: Either[String, FsInstance],
    vwParams: String,
    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    finalizer: Double => B,
    numMissingThreshold: Option[Int] = None,
    spline: Option[Spline] = None)(implicit private[this] val scb: ScoreConverter[B])
extends BaseModel[A, B]
   with RegressionFeatures[A]
   with Logging {

    /**
     * The object responsible to taking the computed features and turning it into a VW input formatted string.
     * This is a transient lazy val so that we can properly serialize
     */
    private[this] val vwRowCreator =
        new VwRowCreator(SparseFeatureExtractorFunction(featureNames zip featureFunctions), defaultNs, namespaces, None)

    @transient private[this] lazy val modelLocationAndDelete = {
        VwJniModel.copyModelToLocal(modelId.getId(), vwModel) fold (f => (f, false), f => (f, true))
    }

    @transient private[this] lazy val allVwParams =
        VwJniModel.updatedVwModelParams(modelId.getId(), modelLocationAndDelete._1, vwParams)

    @transient private[this] lazy val model = {
        val m = VwJniModel.createVwJniModel(allVwParams)
        if (modelLocationAndDelete._2) {
            modelLocationAndDelete._1.delete()
        }
        m
    }

    /**
     * Used for testing.
     * @return
     */
    private[jni] def finalVwParams = allVwParams

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
        require(model != null)
        require(allVwParams != null)
    }

    /** Produce a score.
      * @param a an input to the model representing covariate data.
      * @param audit Whether the second field of the result Tuple2 should be Some (true) or None (false)
      * @return a Tuple2 whose first field represents a simple version of the score, the second field (that should be
      *         a Some instance if audit is true) is a more involved reporting of the score including errors and all
      *         sub-model scores.
      */
     override private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
        generateVwInput(a) match {
            case Left(missing) => failure(Seq(s"Too many features with missing variables: ${missing.count(_._2.nonEmpty)}"), getMissingVariables(missing))
            case Right(vwIn) =>
                Try { model.predict(vwIn) } match {
                    case Success(y) => success(finalizer(spline.map(s => s(y)).getOrElse(y)))
                    case Failure(ex) => failure(Seq(ex.getMessage))
                }
        }
    }

    // TODO: Figure out how to make the missing features fast.
    def getMissingVariables(missing: scm.Map[String, Seq[String]]): Seq[String] =
        missing.unzip._2.foldLeft(Set.empty[String])(_ ++ _).toIndexedSeq.sorted

    /**
     * Get a VW line (on the right) or a map of missing features (on the left) if there was too much
     * missing data to form a prediction.
     * @param a input
     * @return a result
     */
    private[jni] def generateVwInput(a: A): Either[scm.Map[String, Seq[String]], String] = {
        val Features(features, missing, missingOk) = constructFeatures(a)
        if (missingOk) Right(vwRowCreator.unlabeledVwInput(features).toString)
        else           Left(missing)
    }

    /**
     * Close the underlying VW model.
     */
    override def close(): Unit = model.close()
}

object VwJniModel extends ParserProviderCompanion with VwJniModelJson with Logging {

    private[this] val initialRegressorPresent = """^(.*\s)?-i.*$"""

    override def parser: ModelParser = Parser

    object Parser extends ModelParserWithSemantics with EitherHelpers { self =>
        val modelType = "VwJNI"

        override def modelJsonReader[A, B: JsonReader: ScoreConverter](semantics: Semantics[A]): JsonReader[VwJniModel[A, B]] = new JsonReader[VwJniModel[A, B]] {
            override def read(json: JsValue): VwJniModel[A, B] = {
                val vw = json.convertTo[VwJNIAst]

                features(vw.features.toSeq, semantics) match {
                    case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
                    case Right(featureMap) =>
                        val (names, functions) = featureMap.toIndexedSeq.unzip

                        // Conversion function.
                        val cf = {
                            implicit val riB = implicitly[ScoreConverter[B]].ri
                            TypeCoercion[Double, B] getOrElse {
                                throw new DeserializationException(s"Couldn't find conversion function to ${RefInfoOps.toString[B]}")
                            }
                        }

                        val vwParams = vw.vw.params.map(_.fold(_.mkString(" "), identity)) getOrElse ""

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

                        val defaultNs = (indices.keySet -- (Set.empty[String] /: nssRaw)(_ ++ _._2)).flatMap(indices.get).toList

                        VwJniModel(vw.modelId, vw.vw.model, vwParams, names, functions, defaultNs, nss, cf, vw.numMissingThreshold, vw.spline)
                }
            }
        }

        // TODO: Copied from RegressionModel.  Refactor for reuse.
        private[this] def features[A](featureMap: Seq[(String, Spec)], semantics: Semantics[A]) =
            mapSeq(featureMap) {
                case (k, Spec(spec, default)) =>
                    semantics.createFunction[Iterable[(String, Double)]](spec, default).
                        left.map { Seq(s"Error processing spec '$spec'") ++ _ }. // Add the spec that errored.
                        right.map { f => (k, f) }
            }
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
     * Takes the model ID and base64-encoded binary VW model and creates a temp file with the decoded data
     * and returns the file handle.
     * @param modelId numeric model ID
     * @param b64EncodedVwModel the base64-encoded binary VW model
     * @return File object for the newly created temp file
     */
    private[jni] def allocateModel(modelId: Long, b64EncodedVwModel: String): File = {
        val decoded = Base64.decodeBase64(b64EncodedVwModel)
        val f = File.createTempFile(s"aloha.vw.$modelId.", ".model")
        f.deleteOnExit()
        val fos = new FileOutputStream(f)
        try {
            fos.write(decoded)
        }
        finally {
            IOUtils closeQuietly fos
        }
        f
    }

    private[jni] def updatedVwModelParams(modelId: Long, vwModel: File, vwParams: String): String = {
        val initialRegressorParam = s" -i ${vwModel.getCanonicalPath} "

        if (vwParams matches initialRegressorPresent)
            throw new IllegalArgumentException(s"For model $modelId, initial regressor (-i) vw parameter supplied and model provided.")

        initialRegressorParam + vwParams
    }

    /**
     * Create a vw.VW object.
     * @param allVwParams the parameters passed to the vw.VW JNI wrapper
     * @return a vw.VW JNI wrapper model.
     */
    private[jni] def createVwJniModel(allVwParams: String): VW = new VW(allVwParams)

    /**
     * Create a vw.VW object from a base64-encoded binary VW model and a set of VW parameters.
     * @param modelId A numeric model ID
     * @param vwModel the file pointing to the binary VW model
     * @param vwParams the parameters passed to the vw.VW JNI wrapper
     * @return a vw.VW JNI wrapper model.
     */
    private[jni] def createVwJniModel(modelId: Long, vwModel: File, vwParams: String): (VW, String) = {
        val initialRegressorParam = s" -i ${vwModel.getCanonicalPath} "

        if (vwParams matches initialRegressorPresent)
            throw new IllegalArgumentException(s"For model $modelId, initial regressor (-i) vw parameter supplied and model provided.")

        val finalParams = initialRegressorParam + vwParams
        val vwJniModel = new VW(finalParams)
        (vwJniModel, finalParams)
    }

    /**
     * This copies non-local-file content to a local temp file and returns the file name.  If
     * @param modelId the model ID
     * @param vwModel either the FsInstance pointing to the VW model or the Base64 encoded VW model.
     * @return return Right if a temp file was created, Left(vwModel) otherwise.
     */
    private[jni] def copyModelToLocal(modelId: Long, vwModel: Either[String, FsInstance]): Either[File, File] = {
      vwModel match {
        case Left(b64EncodedVwModel) => Left(allocateModel(modelId, b64EncodedVwModel))
        case Right(fsInstance) => fsInstance.localFile.toLeft {
          val tmpFile = File.createTempFile(s"aloha.vw.$modelId.", ".model")

          // While there will be an attempt to clean up the temp immediately after use, this is kept as an
          // extra precaution in case something goes wrong.  Then there is still a change that the file will
          // get cleaned up.
          tmpFile.deleteOnExit()

          val is = fsInstance.inputStream
          val os = new FileOutputStream(tmpFile)
          try {
            IOUtils.copyLarge(is, os)
          }
          finally {
            IOUtils.closeQuietly(is)
            IOUtils.closeQuietly(os)
          }

          tmpFile
        }
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
    def json(spec: FsInstance,
             model: FsInstance,
             id: ModelId,
             vwArgs: Option[String],
             externalModel: Boolean = false,
             numMissingThreshold: Option[Int] = None,
             notes: Option[Seq[String]] = None,
             spline: Option[ConstantDeltaSpline] = None): JsValue = {

        val js = spec match {
            case fs: FileFsInstance => StringReadable.fromFile(fs.fileObj).parseJson
            case fs: Vfs1FsInstance => StringReadable.fromVfs1(fs.fileObj).parseJson
            case fs: Vfs2FsInstance => StringReadable.fromVfs2(fs.fileObj).parseJson
        }
        val vw = js.convertTo[VwUnlabeledJson]
        json(vw, model, id, vwArgs, externalModel, numMissingThreshold, notes, spline)
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
             model: FsInstance,
             id: ModelId,
             vwArgs: Option[String],
             externalModel: Boolean,
             numMissingThreshold: Option[Int],
             notes: Option[Seq[String]],
             spline: Option[ConstantDeltaSpline]): JsValue = {

        def escape(s: String) = s.replaceAllLiterally("\\", "\\\\").replaceAllLiterally("\"", "\\\"")

        val features = ListMap(vw.features.map(f => f.name -> f.toModelSpec):_*)
        val ns = vw.namespaces.map(nss => ListMap(nss.map(n => n.name -> n.features):_*))

        // TODO: If this doesn't work, use commons-lang3 StringEscapeUtils.unescapeJava for unescaping.
        //       Removed commons-lang3 as a dependency because it's only used in 2 places.  Here and
        //       aloha-core CsvModelRunner class.  Here's how it was originally.
        //
        //         val vwParams = Option(vwArgs).filter(_.trim.nonEmpty).map(args => Right(StringEscapeUtils.escapeJson(args)))
        val vwParams = vwArgs.filter(_.trim.nonEmpty).map(args => Right(escape(args)))

        val vwObj = if (externalModel) Vw(Right(model), vwParams)
                    else {
                        val b64Model = VwJniModel.readBinaryVwModelToB64String(model.inputStream)
                        Vw(Left(b64Model), vwParams)
                    }

        VwJNIAst(VwJniModel.parser.modelType, id, features, vwObj, ns, numMissingThreshold, notes, spline).toJson
    }
}
