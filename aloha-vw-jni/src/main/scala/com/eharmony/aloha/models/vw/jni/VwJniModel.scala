package com.eharmony.aloha.models.vw.jni

import java.io._

import com.eharmony.aloha.dataset.SparseFeatureExtractorFunction
import com.eharmony.aloha.dataset.vw.unlabeled.VwSpec
import com.eharmony.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.aloha.id.ModelIdentity
import com.eharmony.aloha.models.reg.json.Spec
import com.eharmony.aloha.models.reg.{RegressionFeatures, Spline}
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
import spray.json.{DeserializationException, JsValue, JsonReader}
import vw.VW

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}


/**
 * Model that delegates to a VW JNI model.
 * @param modelId a modelId
 * @param b64EncodedVwModel a base64-encoded VW binary model.
 * @param vwParams VW parameters.  These are the same as the ones that would be passed on the command line,
 *                 directly to VW.
 * @param featureNames names of features (parallel to featureFunctions)
 * @param featureFunctions functions to extract values from the input value
 * @param defaultNs indices of features that will be placed in the default VW namespace
 * @param namespaces mapping from namespace name to indices of features that will be placed in the namespace
 * @param finalizer a function that transforms the native VW output type (Float) to B
 * @param numMissingThreshold A threshold dictating how many missing features to allow before making the
 *                            prediction fail.  See [[com.eharmony.aloha.models.reg.RegressionFeatures.numMissingThreshold]]
 * @param scb a score converter
 * @tparam A model input type
 * @tparam B model output type
 * @author R M Deak
 */
final case class VwJniModel[-A, +B](
    modelId: ModelIdentity,
    b64EncodedVwModel: String,
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
    private[this] val vwSpec =
        new VwSpec(SparseFeatureExtractorFunction(featureNames zip featureFunctions), defaultNs, namespaces, None)

    /**
     * The backing VW instance doing all of the regression work.
     */
    @transient private[this] lazy val model = VwJniModel.getModel(modelId.getId(), b64EncodedVwModel, vwParams)

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
        require(vwSpec != null)
        require(model != null)
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
        if (missingOk) Right(vwSpec.unlabeledVwInput(features).toString)
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

    /**
     * Create a vw.VW object from a base64-encoded binary VW model and a set of VW parameters.
     * @param modelId A numeric model ID
     * @param b64EncodedVwModel the base64-encoded binary VW model
     * @param vwParams the parameters passed to the vw.VW JNI wrapper
     * @return a vw.VW JNI wrapper model.
     */
    private[jni] def getModel(modelId: Long, b64EncodedVwModel: String, vwParams: String): VW = {
        val m = allocateModel(modelId, b64EncodedVwModel)
        val initialRegressorParam = s" -i ${m.getCanonicalPath} "

        if (vwParams matches initialRegressorPresent)
            throw new IllegalArgumentException(s"For model $modelId, initial regressor (-i) vw parameter supplied and model provided.")

        val finalParams = initialRegressorParam + vwParams
        val vwJniModel = new VW(finalParams)
        vwJniModel
    }
}
