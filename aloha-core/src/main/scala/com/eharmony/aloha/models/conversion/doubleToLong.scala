package com.eharmony.aloha.models.conversion

import java.{lang => jl}

import com.eharmony.aloha.audit.Auditor
import com.eharmony.aloha.factory._
import com.eharmony.aloha.id.ModelIdentityJson.modelIdJsonFormat
import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import com.eharmony.aloha.models.Submodel
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.util.Logging
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, JsonFormat, JsonReader, RootJsonFormat}

/**
 * A model that converts from Scala Doubles to Scala Longs by scaling, translating, then rounding.
 * @param modelId a model id
 * @param submodel a submodel whose values should be translated
 * @param scale a scale factor
 * @param translation a translation
 * @param round whether to round (true) or floor (false)
 * @tparam A model input type
 */
case class DoubleToLongModel[U, -A, +B <: U](
    modelId: ModelIdentity,
    submodel: Submodel[Double, A, U],
    auditor: Auditor[U, Long, B],
    scale: Double = 1,
    translation: Double = 0,
    clampLower: Long = Long.MinValue,
    clampUpper: Long = Long.MaxValue,
    round: Boolean = false
) extends ConversionModel[U, Double, Long, A, B]
     with Logging {

  require(clampLower <= clampUpper, s"clampLower ($clampLower) must be less than or equal to clampUpper ($clampUpper)")

  if (scale == 0) warn(s"model ${modelId.getId()} will allows return a constant ${rounder(scale)}.")

  private[this] val rounder = if (round) (_: Double).round
                              else (_: Double).toLong

  val conversion: Double => Long =
    x => math.max(clampLower, math.min(rounder(scale * x + translation), clampUpper))
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
case class DoubleToJavaLongModel[U, -A, +B <: U](
    modelId: ModelIdentity,
    submodel: Submodel[Double, A, U],
    auditor: Auditor[U, jl.Long, B],
    scale: Double = 1,
    translation: Double = 0,
    clampLower: Long = Long.MinValue,
    clampUpper: Long = Long.MaxValue,
    round: Boolean = false
) extends ConversionModel[U, Double, jl.Long, A, B]
  with Logging {

  require(clampLower <= clampUpper, s"clampLower ($clampLower) must be less than or equal to clampUpper ($clampUpper)")

  if (scale == 0) warn(s"model ${modelId.getId()} will allows return a constant ${rounder(scale)}.")

  private[this] val rounder = if (round) (_: Double).round
                              else (_: Double).toLong

  val conversion: Double => jl.Long =
    x => jl.Long.valueOf(math.max(clampLower, math.min(rounder(scale * x + translation), clampUpper)))
}

object DoubleToLongModel extends ParserProviderCompanion {

  object Parser extends ModelSubmodelParsingPlugin {
    val modelType = "DoubleToLong"

    case class DoubleToLongAst(
        modelId: ModelId,
        submodel: JsValue,
        scale: Option[Double],
        translation: Option[Double],
        clampLower: Option[Long],
        clampUpper: Option[Long],
        round: Option[Boolean])

    implicit val doubleToLongAstFormat: RootJsonFormat[DoubleToLongAst] = jsonFormat7(DoubleToLongAst)

    override def commonJsonReader[U, N, A, B <: U](
        factory: SubmodelFactory[U, A],
        semantics: Semantics[A],
        auditor: Auditor[U, N, B])
       (implicit r: RefInfo[N], jf: JsonFormat[N]): Option[JsonReader[ConversionModel[U, Double, N, A, B]]] = {

      if (r != RefInfo[Long] && r != RefInfo[jl.Long])
        None
      else {
        Some(new JsonReader[ConversionModel[U, Double, N, A, B]] {
          override def read(json: JsValue): ConversionModel[U, Double, N, A, B] = {

            val ast = json.convertTo[DoubleToLongAst]

            val submodelTry = factory.submodel[Double](ast.submodel)

            val modelTry =
              submodelTry.map(sm => {
                val scale = ast.scale.getOrElse(1.0)
                val translation = ast.translation.getOrElse(0.0)
                val round = ast.round.getOrElse(false)
                val clampLower = ast.clampLower.getOrElse(Long.MinValue)
                val clampUpper = ast.clampUpper.getOrElse(Long.MaxValue)

                // This is all-encompassing because we checked above that the type is either java or scala.
                if (r == RefInfo[Long]) {
                  val aud = auditor.asInstanceOf[Auditor[U, Long, B]]
                  DoubleToLongModel(ast.modelId, sm, aud, scale, translation, clampLower, clampUpper, round).asInstanceOf[ConversionModel[U, Double, N, A, B]]
                } else {
                  val aud = auditor.asInstanceOf[Auditor[U, jl.Long, B]]
                  DoubleToJavaLongModel(ast.modelId, sm, aud, scale, translation, clampLower, clampUpper, round).asInstanceOf[ConversionModel[U, Double, N, A, B]]
                }
              })

            modelTry.get
          }
        })
      }
    }
  }

  def parser: ModelParser = Parser
}
