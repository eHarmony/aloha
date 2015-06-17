package com.eharmony.matching.aloha.models.vw.jni

import java.io.{InputStream, File, FileOutputStream}

import com.eharmony.matching.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.models.reg.RegressionFeatures
import com.eharmony.matching.aloha.models.{BaseModel, TypeCoercion}
import com.eharmony.matching.aloha.reflect._
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.{EitherHelpers, Logging}
import com.eharmony.matching.featureSpecExtractor.SparseFeatureExtractorFunction
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.VwSpec
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import spray.json.{DeserializationException, JsValue, JsonReader}
import vw.VW

import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}


/**
 * Model that delegates to a VW JNI model.
 * @param modelId a modelId
 * @param model VW JNI instance
 * @param featureNames names of features (parallel to featureFunctions)
 * @param featureFunctions functions to extract values from the input value
 * @param defaultNs indices of features that will be placed in the default VW namespace
 * @param namespaces mapping from namespace name to indices of features that will be placed in the namespace
 * @param finalizer a function that transforms the native VW output type (Float) to B
 * @param numMissingThreshold A threshold dictating how many missing features to allow before making the
 *                            prediction fail.  See [[com.eharmony.matching.aloha.models.reg.RegressionFeatures.numMissingThreshold]]
 * @param scb a score converter
 * @tparam A model input type
 * @tparam B model output type
 * @author R M Deak
 */
final case class VwJniModel[-A, +B](
    modelId: ModelIdentity,
    private val model: VW,
    featureNames: sci.IndexedSeq[String],
    featureFunctions: sci.IndexedSeq[GenAggFunc[A, Iterable[(String, Double)]]],
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    finalizer: Float => B,
    numMissingThreshold: Option[Int] = None)(implicit private[this] val scb: ScoreConverter[B])
extends BaseModel[A, B]
   with RegressionFeatures[A]
   with Logging {

    private[this] val vwSpec = new VwSpec(SparseFeatureExtractorFunction(featureNames zip featureFunctions), defaultNs, namespaces, None)

    {
        require(
            featureNames.size == featureFunctions.size,
            s"featureNames.size (${featureNames.size}}) != featureFunctions.size (${featureFunctions.size}})")

        val req = sci.BitSet(featureFunctions.indices:_*)
        val act = (sci.BitSet(defaultNs:_*) /: namespaces)(_ ++ _._2)
        require(
            req == act,
            s"defaultNamespace and namespaces must cover all indices (0 until ${featureFunctions.size}).  Missing indices: ${(req -- act).map(i => s"$i='${featureNames(i)}'").mkString(", ")}.")
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
                    case Success(y) => success(finalizer(y))
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
    object Parser extends ModelParserWithSemantics with EitherHelpers { self =>
        val modelType = "VwJNI"
        private[this] val initialRegressorPresent = """^(.*\s)?-i.*$"""

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
                            TypeCoercion[Float, B] getOrElse {
                                throw new DeserializationException(s"Couldn't find conversion function to ${RefInfoOps.toString[B]}")
                            }
                        }

                        val initialRegressorParam = vw.vw.model.fold("") { vwModel =>
                            val m = allocateModel(vw.modelId.id, vwModel)
                            s" -i ${m.getCanonicalPath} "
                        }

                        // If VW params are unspecified, use empty string.
                        val vwParams = vw.vw.getParams.fold(_.mkString(" "), identity)

                        if (initialRegressorParam.nonEmpty && vwParams.matches(initialRegressorPresent)) {
                            throw new IllegalArgumentException(s"For model ${vw.modelId.id}, initial regressor (-i) vw parameter supplied and model provided.")
                        }

                        val finalParams = initialRegressorParam + vwParams

                        // TODO: Map failure to more specialized exception types.  For instance: Try { new VWScorer(vwParams) }.recoverWith{case ex: Error if ex.getMessage.startsWith("unrecognised option") => Failure(new Ex(ex.getMessage, ex))}
                        val vwJniModel = new VW(finalParams)

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
                        VwJniModel(vw.modelId, vwJniModel, names, functions, defaultNs, nss, cf, vw.numMissingThreshold)
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

    private[jni] def allocateModel(modelId: Long, modelB64: String): File = {
        val decoded = Base64.decodeBase64(modelB64)
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

    private[jni] def readModel(in: InputStream, close: Boolean = true): String = {
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

    private[this] val VwLinkFunctions = Seq("identity", "logistic", "glf1")

    override def parser: ModelParser = Parser
}
