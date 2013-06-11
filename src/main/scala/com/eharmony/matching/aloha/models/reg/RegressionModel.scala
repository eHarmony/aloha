package com.eharmony.matching.aloha.models.reg

import java.{lang => jl}
import scala.language.{higherKinds, implicitConversions}
import scala.collection.mutable.{Map => MMap}

import com.eharmony.matching.aloha.models.Model
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.basic.ModelOutput
import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.factory.{ModelParserWithSemantics, ModelParser, ParserProviderCompanion}
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.factory.pimpz.JsValuePimpz
import com.eharmony.matching.aloha.util.EitherHelpers
import com.eharmony.matching.aloha.reflect.{RefInfoOps, RefInfo}
import grizzled.slf4j.Logging

/** A regression model capable of doing not only linear regression but polynomial regression in general.
  *
  * {{{
  * val regImp = "com.eharmony.matching.aloha.models.reg.RegressionModelValueToTupleConversions._"
  * val compiler = ...
  * val plugin = ...
  * val imports: Seq[String] = ...
  * val s = CompiledSemantics(compiler, plugin, imports :+ regImp)
  * }}}
  *
  * This is useful because these conversions allow implicit conversion function from some of the AnyVal types and
  * Options of AnyVal types to Iterable[(String, Double)].  This is useful because specifying features in the JSON
  * spec like:
  * {{{
  * {
  *   ...
  *   "features": {
  *     "intercept": "-3",
  *     "income": "${user.profile.income}"
  *   }
  * }
  * }}}
  *
  * into sequences like:
  *
  * {{{
  * val interceptFeature = Iterable(("intercept", 3.0))  // AND
  * val incomeFeature = Iterable(("income", [the income value converted to a double]))
  * }}}
  *
  * For more information, see [[com.eharmony.matching.aloha.models.reg.RegressionModelValueToTupleConversions]].
  *
  * @param modelId An identifier for the model.  User in score and error reporting.
  * @param features map of feature name to feature generator
  * @param beta representation of the regression model parameters.
  * @param invLinkFunction a function applied to the inner product of the input vector and weight vector.
  * @param spline an optional calibration spline to
  *               [[http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.29.3039 Obtaining calibrated probability
  *                 estimates from decision trees and naive Bayesian classifiers]], Zadrozny, Elkan (ICML, 2001).  This
  *                 is applied prior to invLinkFunction
  * @param numMissingThreshold if provided, we check whether the threshold is exceeded.  If so, return an error instead
  *                            of the computed score.  This is for missing data situations.
  * @param evidence$1 converter from type instance of B to a [[com.eharmony.matching.aloha.score.Scores.Score]]
  * @tparam A model input type
  * @tparam B model input type
  */
case class RegressionModel[-A, +B: ScoreConverter](
        modelId: ModelIdentity,
        features: Map[String, GenAggFunc[A, Iterable[(String, Double)]]],
        beta: PolynomialEvaluationAlgo,
        invLinkFunction: Double => B,
        spline: Option[Spline],
        numMissingThreshold: Option[Int])
    extends Model[A, B]
    with Logging {

    private[this] val (featuresNames, featureFunctions) = features.toIndexedSeq.unzip

    debug({
        val rawFeatureDescriptors = (for {
            ff <- featureFunctions
            a <- ff.accessors
        } yield a.descriptor).sorted
        "raw feature names: " + rawFeatureDescriptors.mkString(",")
    })



    /** Get the score.
      * @param a the model input value.
      * @param audit whether to audit the output.
      * @return
      */
    private[aloha] def getScore(a: A)(implicit audit: Boolean): (ModelOutput[B], Option[Score]) = {
        val (x, missing, missingOk) = constructFeatures(a)

        debug("x\n\t" + featuresNames.zip(x).map{ case(name, f) => s"$name -> $f"}.mkString("\n\t"))

        // Before determining the inner product, we know whether we actually should compute it or whether a
        // data error has occurred the will prevent the computation.
        val out =
            if (missingOk) {
                val eta = beta at x
                debug(s"eta: $eta")
                val splinedEta = spline.map(_(eta)) getOrElse eta
                debug(s"splined eta: $splinedEta")
                val mu = invLinkFunction(splinedEta)              // Currently, really just a casting operation.
                debug(s"mu: $mu")
                ModelOutput(mu)
            }
            else ModelOutput.fail(
                Seq[String]("Missing too much data in features: " + missing.keys.toIndexedSeq.sorted),
                missing.values.flatten)

        val s = if (audit) Option(toScoreWithMissingFeatures(out, missing.values.flatten)) else None
        (out, s)
    }

    /** Extract the features from the raw data.  Intentionally, ''protected[this] final'' so that we can extend this
      * class
      * @param a raw input data of the model input type.
      * @return a Tuple3 with the following:
      *           1 the transformed input vector
      *           1 the map of bad features to the missing values in the raw data that were needed to compute the feature
      *           1 whether the amount of missing data is acceptable to still continue
      */
    protected[this] final def constructFeatures(a: A): (IndexedSeq[Iterable[(String, Double)]], MMap[String, Seq[String]], Boolean) = {
        val missing = MMap.empty[String, Seq[String]]
        val n = features.size
        val f = new Array[Iterable[(String, Double)]](n)
        var i = 0

        while(i < n) {
            val name = featuresNames(i)
            f(i) = featureFunctions(i)(a).map(p => (name + p._1, p._2))

            // If the feature is empty, it can't contribute to the inner product.  If it can't contribute to the
            // inner product but appears in the inner product specification, there are two possibilities:
            //
            //   1) The specifier doesn't care about performance and needlessly added a NoOp.
            //   2) The feature could emit a value because data necessary to do so is missing.
            //
            // In either case, we take those opportunities to check for missing data and assume the performance
            // hit is acceptable.
            if (f(i).isEmpty)
                missing += (featureFunctions(i).specification -> featureFunctions(i).accessorOutputMissing(a))
            i += 1
        }

        val numMissingOk = numMissingThreshold map {missing.size < _} getOrElse true

        // If we are going to err out, allow a linear scan (with repeated work so that we can get richer error
        // diagnostics.
        if (!numMissingOk) {
            i = 0
            while(i < n) {
                missing += (featureFunctions(i).specification -> featureFunctions(i).accessorOutputMissing(a))
                i += 1
            }
        }

        (new collection.mutable.WrappedArray.ofRef(f), missing, numMissingOk)
    }
}

object RegressionModel extends ParserProviderCompanion with JsValuePimpz {
    import spray.json._
    import spray.json.DefaultJsonProtocol._

    protected[this] type M[-A, +B] = RegressionModel[A, B]

    private[this] case class Spec(spec: String, default: Seq[(String, Double)] = Nil)
    private[this] val specJsonFormat = jsonFormat2(Spec)

    private[this] implicit object FeatureSpecFormat extends JsonFormat[Spec] {
        def write(o: Spec) = throw new SerializationException("Spec serialization not supported")
        def read(json: JsValue) = json match {
            case JsString(s) => Spec(s)
            case o : JsObject => o.convertTo[Spec](specJsonFormat)
            case e => throw new DeserializationException(s"unexpected feature $e")
        }
    }

    private[this] case class Hof(features: Map[String, Seq[String]], wt: Double)
    private[this] case class RegData(features: Map[String, Spec], weights: Map[String, Double], higherOrderFeatures: Option[Seq[Hof]], spline: Option[ConstantDeltaSpline], numMissingThreshold: Option[Int])
    private[this] implicit val hofJsonFormat = jsonFormat2(Hof)
    private[this] implicit val regSplineJsonFormat = jsonFormat(ConstantDeltaSpline, "min", "max", "knots")
    private[this] implicit val regDataJsonFormat = jsonFormat5(RegData)

    private[this] class Parser extends ModelParserWithSemantics[M] with EitherHelpers {
        val modelType = "Regression"

        /**
          *
          * @param semantics This reader requires semantics to be provided (some).  Otherwise, an error will occur. This
          *                  is because the regression models create functions for each feature in the model and
          *                  function creation is performed by the semantics.
          * @tparam A input type of the model
          * @tparam B output type of the model
          * @tparam C RegressionModel[A, B] (needed for dealing with proper type variance)
          * @return
          */
        def modelJsonReader[A, B: JsonReader: ScoreConverter, C >: M[A, B]](semantics: Option[Semantics[A]]) = new JsonReader[C] {
            def read(json: JsValue): RegressionModel[A, B] = {

                val modelId = getModelId(json) getOrElse { throw new DeserializationException("No model ID available: " + json) }

                val sem = semantics getOrElse { throw new DeserializationException("Required semantics for no semantics") }

                // Get the metadata necessary to create the model.
                val d = json.convertTo[RegData]

                // Turn the map of features into a Seq to fix the order for all subsequent operations because they
                // need a common understanding of the indices for the features.
                val featureMap: Seq[(String, Spec)] = d.features.toSeq
                val featureNameToIndex: Map[String, Int] = featureMap.map(_._1).zipWithIndex.toMap

                // This is the weight vector.
                val beta = getBeta(d.features.size, d.weights, higherOrderFeatures(d, featureNameToIndex))

                // Get the function that coerces to the output type.
                val rib = implicitly[ScoreConverter[B]].ri
                val cf = conversionFunction[B](rib) getOrElse {
                    throw new DeserializationException("Couldn't find conversion function for RegressionModel with output type: " + RefInfoOps.toString(rib))
                }

                // These are the features.  Do them last because they are the slowest.
                val f = features(featureMap, sem).fold(f => throw new DeserializationException(f.mkString("\n")), identity)
                val m = RegressionModel[A, B](modelId, f, beta, cf, d.spline, d.numMissingThreshold)
                m
            }
        }

        private[this] def conversionFunction[B: RefInfo] = {
            import doubleFunctions._
            val a = RefInfo[B] match {
                case RefInfo.Byte              => Option(doubleToByteFunction.asInstanceOf[(Double => B)])
                case RefInfo.Short             => Option(doubleToShortFunction.asInstanceOf[(Double => B)])
                case RefInfo.Int               => Option(doubleToIntFunction.asInstanceOf[(Double => B)])
                case RefInfo.Long              => Option(doubleToLongFunction.asInstanceOf[(Double => B)])
                case RefInfo.Float             => Option(doubleToFloatFunction.asInstanceOf[(Double => B)])
                case RefInfo.Double            => Option(doubleToDoubleFunction.asInstanceOf[(Double => B)])

                case RefInfo.JavaByte          => Option(doubleToJavaByteFunction.asInstanceOf[(Double => B)])
                case RefInfo.JavaShort         => Option(doubleToJavaShortFunction.asInstanceOf[(Double => B)])
                case RefInfo.JavaInteger       => Option(doubleToJavaIntFunction.asInstanceOf[(Double => B)])
                case RefInfo.JavaLong          => Option(doubleToJavaLongFunction.asInstanceOf[(Double => B)])
                case RefInfo.JavaFloat         => Option(doubleToJavaFloatFunction.asInstanceOf[(Double => B)])
                case RefInfo.JavaDouble        => Option(doubleToJavaDoubleFunction.asInstanceOf[(Double => B)])

                case x if x == RefInfo[String] => Option(doubleToStringFunction.asInstanceOf[(Double => B)])
            }
            a
        }

        /** Translate the feature specification into features.  This is done in a short circuiting way so that it
          * stops when the any feature cannot be produced.
          *
          * @param featureMap a map of feature name to feature specification
          * @param semantics a semantics with which feature specifications should be interpretted.
          * @tparam A model input type
          * @return
          */
        private[this] def features[A](featureMap: Seq[(String, Spec)], semantics: Semantics[A]) =
            mapSeq(featureMap){
                case (k, Spec(spec, default)) =>
                    semantics.createFunction[Iterable[(String, Double)]](spec, Option(default)).right.map(f => (k, f))
            }.right.map(_.toMap)

        /** Translate the specification of higher order features to something a
          * [[com.eharmony.matching.aloha.models.reg.PolynomialEvaluator]].builder can understand.
          * @param d regression model metadata
          * @param featureNameToIndex mapping from feature name to index in the vector of features.
          * @return
          */
        private[this] def higherOrderFeatures(d: RegData, featureNameToIndex: Map[String, Int]): Seq[(Seq[(String, Int)], Double)] = {
            d.higherOrderFeatures.map(s =>
                s.map(h => {
                    val f = h.features.toSeq flatMap { case (k,v) => {
                        val kI = featureNameToIndex(k)  // Will Throw if not present.
                        v.zip(Stream continually kI)
                    }}
                    (f, h.wt)
                })
            ) getOrElse Nil
        }


        /** Construct a polynomial evaluator given the first order weights (in weights field) and the higher order
          * features.
          * @param n number of features in the feature (generation) vector.
          * @param foWeights the weight map for first order weights.
          * @param higherOrderFeatures the higher order features (order > 1)
          * @return
          */
        private[this] def getBeta(n: Int, foWeights: Map[String, Double], higherOrderFeatures: TraversableOnce[(TraversableOnce[(String, Int)], Double)]) = {
            val p = PolynomialEvaluator.builder.
                addAllFirstOrder(foWeights, n).
                ++= (higherOrderFeatures).
                result()
            p
        }
    }

    val parser = new Parser().asInstanceOf[ModelParser[M]]

    private[this] object doubleFunctions {
        val doubleToByteFunction   = (_: Double).toByte
        val doubleToShortFunction  = (_: Double).toShort
        val doubleToIntFunction    = (_: Double).toInt
        val doubleToLongFunction   = (_: Double).toLong
        val doubleToFloatFunction  = (_: Double).toFloat
        val doubleToDoubleFunction = (d: Double) => d
        val doubleToStringFunction = (_: Double).toString

        val doubleToJavaByteFunction   = (d: Double) => jl.Byte.valueOf(d.toByte)
        val doubleToJavaShortFunction  = (d: Double) => jl.Short.valueOf(d.toShort)
        val doubleToJavaIntFunction    = (d: Double) => jl.Integer.valueOf(d.toInt)
        val doubleToJavaLongFunction   = (d: Double) => jl.Long.valueOf(d.toLong)
        val doubleToJavaFloatFunction  = (d: Double) => jl.Float.valueOf(d.toFloat)
        val doubleToJavaDoubleFunction = (d: Double) => jl.Double.valueOf(d)
    }
}
