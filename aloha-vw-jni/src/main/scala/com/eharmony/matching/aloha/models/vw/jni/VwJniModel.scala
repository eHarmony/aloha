package com.eharmony.matching.aloha.models.vw.jni


import java.text.DecimalFormat
import java.{lang => jl}

import com.eharmony.matching.aloha.factory.{ModelParser, ModelParserWithSemantics, ParserProviderCompanion}
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.models.BaseModel
import com.eharmony.matching.aloha.models.reg.RegressionFeatures
import com.eharmony.matching.aloha.reflect._
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.util.{EitherHelpers, Logging}
import spray.json.{DeserializationException, JsValue, JsonReader}
import vw.VWScorer

import scala.collection.immutable.BitSet
import scala.collection.{immutable => sci, mutable => scm}
import scala.util.{Failure, Success, Try}


final case class VwJniModel[-A, +B: ScoreConverter](
    private val model: vw.VWScorer,
    modelId: ModelIdentity,
    features: sci.IndexedSeq[(String, GenAggFunc[A, Iterable[(String, Double)]])],
    defaultNs: List[Int],
    namespaces: List[(String, List[Int])],
    finalizer: Float => B,
    numMissingThreshold: Option[Int] = None)
extends BaseModel[A, B]
   with RegressionFeatures[A]
   with Logging {

    protected[this] override val (featureNames, featureFunctions) = features.unzip

    {
        val req = BitSet(featureFunctions.indices:_*)
        val act = (BitSet(defaultNs:_*) /: namespaces)(_ ++ _._2)
        require(req == act, s"defaultNamespace and namespaces must cover all indices (0 until ${featureFunctions.size}).  Missing ${(req -- act).mkString(",")}")
    }

    import VwJniModel._


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
                Try { model.getPrediction(vwIn) } match {
                    case Success(y) => success(finalizer(y))
                    case Failure(ex) => failure(Seq(ex.getMessage))
                }
        }
    }

    // TODO: Figure out how to make the missing features fast.
    def getMissingVariables(missing: scm.Map[String, Seq[String]]): Seq[String] =
        missing.unzip._2.foldLeft(Set.empty[String])(_ ++ _).toIndexedSeq.sorted

    private[jni] def generateVwInput(a: A): Either[scm.Map[String, Seq[String]], String] = {
        val Features(features, missing, missingOk) = constructFeatures(a)

        if (!missingOk) Left(missing)
        else {
            val b = new StringBuilder

            if (defaultNs.nonEmpty) {
                b.append("| ")
                defaultNs foreach { i =>
                    features(i) foreach { case (f, v) =>
                        if (!inEpsilonInterval(v)) {
                            b.append(f)
                            if (!inEpsilonInterval(v - 1)) b.append(":").append(DecimalFormatter.format(v))
                            b.append(" ")
                        }
                    }
                }
            }

            namespaces foreach { case(ns, ind) =>
                b.append("|").append(ns).append(" ")
                ind foreach { i =>
                    features(i) foreach { case (f, v) =>
                        if (!inEpsilonInterval(v)) {
                            b.append(f)
                            if (!inEpsilonInterval(v - 1)) b.append(":").append(DecimalFormatter.format(v))
                            b.append(" ")
                        }
                    }
                }
            }

            Right(b.toString())
        }
    }
}

object VwJniModel extends ParserProviderCompanion with VwJNIModelJson {

    private[jni] val FeatureDecimalDigits = 6
    private[jni] val DecimalFormatter = new DecimalFormat(List.fill(FeatureDecimalDigits)("#").mkString("0.", "", ""))
    private[this] val eps = math.pow(10, -FeatureDecimalDigits) / 2
    private[this] val negEps = -eps
    private[jni] def inEpsilonInterval(x: Double) = negEps < x && x < eps

    object Parser extends ModelParserWithSemantics with EitherHelpers { self =>
        val modelType = "VwJNI"

        private[this] def features[A](featureMap: Seq[(String, Spec)], semantics: Semantics[A]) =
            mapSeq(featureMap) {
                case (k, Spec(spec, default)) =>
                    semantics.createFunction[Iterable[(String, Double)]](spec, default).
                        left.map { Seq(s"Error processing spec '$spec'") ++ _ }. // Add the spec that errored.
                        right.map { f => (k, f) }
            }

        override def modelJsonReader[A, B: JsonReader: ScoreConverter](semantics: Semantics[A]): JsonReader[VwJniModel[A, B]] = new JsonReader[VwJniModel[A, B]] {
            override def read(json: JsValue): VwJniModel[A, B] = {

                implicit val riB = implicitly[ScoreConverter[B]].ri

                val convFunc = conversionFunction[B] getOrElse {
                    throw new DeserializationException(s"Couldn't find conversion function to ${RefInfoOps.toString[B]}")
                }

                val vw = json.convertTo[VwJNIAst]

                features(vw.features.toSeq, semantics) match {
                    case Left(errors) => throw new DeserializationException(errors.mkString("errors: ", "\n        ", ""))
                    case Right(featureMap) =>
                        val vwParams = vw.vw.params.fold(_.mkString(" "), identity)
                        val vwJniModel = new VWScorer(vwParams)
                        val indices = featureMap.unzip._1.view.zipWithIndex.toMap
                        val nss = vw.namespaces.map { case (ns, fs) => (ns, fs.flatMap(indices.get).toList)}.toList
                        val defaultNs = (indices.keySet -- (Set.empty[String] /: vw.namespaces)(_ ++ _._2)).flatMap(indices.get).toList
                        VwJniModel(vwJniModel, vw.modelId, featureMap.toIndexedSeq, defaultNs, nss, convFunc, vw.numMissingThreshold)
                }
            }
        }
    }

    override def parser: ModelParser = Parser

    private[this] def conversionFunction[B: RefInfo] = {
        import floatFunctions._
        val a = RefInfo[B] match {
            case RefInfo.Byte => Option(floatToByteFunction.asInstanceOf[(Float => B)])
            case RefInfo.Short => Option(floatToShortFunction.asInstanceOf[(Float => B)])
            case RefInfo.Int => Option(floatToIntFunction.asInstanceOf[(Float => B)])
            case RefInfo.Long => Option(floatToLongFunction.asInstanceOf[(Float => B)])
            case RefInfo.Float => Option(floatToFloatFunction.asInstanceOf[(Float => B)])
            case RefInfo.Double => Option(floatToDoubleFunction.asInstanceOf[(Float => B)])

            case RefInfo.JavaByte => Option(floatToJavaByteFunction.asInstanceOf[(Float => B)])
            case RefInfo.JavaShort => Option(floatToJavaShortFunction.asInstanceOf[(Float => B)])
            case RefInfo.JavaInteger => Option(floatToJavaIntFunction.asInstanceOf[(Float => B)])
            case RefInfo.JavaLong => Option(floatToJavaLongFunction.asInstanceOf[(Float => B)])
            case RefInfo.JavaFloat => Option(floatToJavaFloatFunction.asInstanceOf[(Float => B)])
            case RefInfo.JavaDouble => Option(floatToJavaDoubleFunction.asInstanceOf[(Float => B)])

            case x if x == RefInfo[String] => Option(floatToStringFunction.asInstanceOf[(Float => B)])
        }
        a
    }

    private[this] object floatFunctions {
        val floatToByteFunction = (_: Float).toByte
        val floatToShortFunction = (_: Float).toShort
        val floatToIntFunction = (_: Float).toInt
        val floatToLongFunction = (_: Float).toLong
        val floatToFloatFunction = (f: Float) => f
        val floatToDoubleFunction = (_: Float).toDouble
        val floatToStringFunction = (_: Float).toString

        val floatToJavaByteFunction = (f: Float) => jl.Byte.valueOf(f.toByte)
        val floatToJavaShortFunction = (f: Float) => jl.Short.valueOf(f.toShort)
        val floatToJavaIntFunction = (f: Float) => jl.Integer.valueOf(f.toInt)
        val floatToJavaLongFunction = (f: Float) => jl.Long.valueOf(f.toLong)
        val floatToJavaFloatFunction = (f: Float) => jl.Float.valueOf(f)
        val floatToJavaDoubleFunction = (f: Float) => jl.Double.valueOf(f)
    }
}
