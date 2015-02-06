package com.eharmony.matching.aloha.models.conversion

import java.{ lang => jl }

import com.eharmony.matching.aloha.util.Logging

import spray.json.{ JsValue, JsonReader }
import spray.json.DefaultJsonProtocol._

import com.eharmony.matching.aloha.id.{ ModelId, ModelIdentity }
import com.eharmony.matching.aloha.id.ModelIdentityJson.modelIdJsonFormat

import com.eharmony.matching.aloha.score.conversions.ScoreConverter
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.DoubleScoreConverter
import com.eharmony.matching.aloha.factory.{ ModelFactory, ModelParser, ParserProviderCompanion }
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect.{ RefInfoOps, RefInfo }
import com.eharmony.matching.aloha.factory.ex.AlohaFactoryException
import com.eharmony.matching.aloha.models.Model

/**
 * A model that converts from Scala Doubles to Scala Longs by scaling, translating, then rounding.
 * @param modelId a model id
 * @param submodel a submodel whose values should be translated
 * @param scale a scale factor
 * @param translation a translation
 * @param round whether to round (true) or floor (false)
 * @tparam A model input type
 */
case class DoubleToLongModel[-A](modelId: ModelIdentity,
  submodel: Model[A, Double],
  scale: Double = 1,
  translation: Double = 0,
  clampLower: Long = Long.MinValue,
  clampUpper: Long = Long.MaxValue,
  round: Boolean = false)
  extends ConversionModel[A, Double, Long] with Logging {

  require(clampLower <= clampUpper, s"clampLower ($clampLower) must be less than or equal to clampUpper ($clampUpper)")

  override val scoreConverter = com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.LongScoreConverter

  if (scale == 0) warn(s"model ${modelId.getId()} will allows return a constant ${rounder(scale)}.")

  private[this] val rounder = if (round) (_: Double).round
  else (_: Double).toLong

  val conversion = (x: Double) => math.max(clampLower, math.min(rounder(scale * x + translation), clampUpper))
}

/**
 * A model that converts from Scala Doubles to Java Longs by scaling, translating, then rounding.
 * @param modelId a model id
 * @param submodel a submodel whose values should be translated
 * @param scale a scale factor
 * @param translation a translation
 * @param round whether to round (true) or floor (false)
 * @tparam A model input type
 */
case class DoubleToJavaLongModel[-A](modelId: ModelIdentity,
  submodel: Model[A, Double],
  scale: Double = 1,
  translation: Double = 0,
  clampLower: Long = Long.MinValue,
  clampUpper: Long = Long.MaxValue,
  round: Boolean = false)
  extends ConversionModel[A, Double, jl.Long] with Logging {

  require(clampLower <= clampUpper, s"clampLower ($clampLower) must be less than or equal to clampUpper ($clampUpper)")

  override val scoreConverter = com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.JavaLongScoreConverter

  if (scale == 0) warn(s"model ${modelId.getId()} will allows return a constant ${rounder(scale)}.")

  private[this] val rounder = if (round) (_: Double).round
  else (_: Double).toLong

  val conversion = (x: Double) => jl.Long.valueOf(math.max(clampLower, math.min(rounder(scale * x + translation), clampUpper)))
}

object DoubleToLongModel extends ParserProviderCompanion {
  object Parser extends ModelParser {

    case class DoubleToLongAst(modelId: ModelId,
      submodel: JsValue,
      scale: Option[Double],
      translation: Option[Double],
      clampLower: Option[Long],
      clampUpper: Option[Long],
      round: Option[Boolean])

    implicit val doubleToLongAstFormat = jsonFormat7(DoubleToLongAst)

    val modelType = "DoubleToLong"

    /**
     * Need semantics
     * need to create submodel of specific type
     *
     * @param factory ModelFactory[Model[_, _] ]
     * @tparam A model input type
     * @tparam B model input type
     * @return
     */
    def modelJsonReader[A, B: JsonReader: ScoreConverter](factory: ModelFactory, semantics: Option[Semantics[A]]) = new JsonReader[ConversionModel[A, Double, B]] {
      def read(json: JsValue) = {
        if (semantics.isEmpty) {
          throw new AlohaFactoryException("No semantics provided.  Need semantics to produce submodel.")
        }

        val riB = implicitly[ScoreConverter[B]].ri

        if (riB != RefInfo[Long] && riB != RefInfo[jl.Long]) {
          throw new AlohaFactoryException(s"DoubleToLong model can only return scala.Long or java.lang.Long. Found ${RefInfoOps.toString(riB)} in JSON:\n$json")
        }

        // Get the values but just the JSON for the submodel.
        val ast = json.convertTo[DoubleToLongAst]

        implicit val riA = semantics.get.refInfoA

        val submodelTry = factory.getModel[A, Double](ast.submodel, semantics)

        val modelTry =
          submodelTry.map(sm => {
            val scale = ast.scale.getOrElse(1.0)
            val translation = ast.translation.getOrElse(0.0)
            val round = ast.round.getOrElse(false)
            val clampLower = ast.clampLower.getOrElse(Long.MinValue)
            val clampUpper = ast.clampUpper.getOrElse(Long.MaxValue)

            // This is all-encompassing because we checked above that the type is either java or scala.
            if (riB == RefInfo[Long]) {
              DoubleToLongModel(ast.modelId, sm, scale, translation, clampLower, clampUpper, round).asInstanceOf[ConversionModel[A, Double, B]]
            } else {
              DoubleToJavaLongModel(ast.modelId, sm, scale, translation, clampLower, clampUpper, round).asInstanceOf[ConversionModel[A, Double, B]]
            }
          })

        modelTry.get
      }
    }
  }

  def parser: ModelParser = Parser
}
