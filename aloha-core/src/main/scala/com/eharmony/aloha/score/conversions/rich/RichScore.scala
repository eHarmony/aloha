package com.eharmony.aloha.score.conversions.rich

import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.Scores.Score.BaseScore.ScoreType
import com.eharmony.aloha.score.Scores.Score.BaseScore.ScoreType.{BOOLEAN, DOUBLE, FLOAT, INT, LONG, STRING, NONE}
import com.eharmony.aloha.score.Scores.Score.ScoreError
import com.eharmony.aloha.score.conversions.{BasicTypeScoreConversions, RelaxedConversions, StrictConversions}
import com.eharmony.aloha.factory.JavaJsonFormats.javaListJsonFormat
import spray.json._
import spray.json.DefaultJsonProtocol.StringJsonFormat

import scala.annotation.switch
import scala.collection.{immutable => sci}
import scala.language.higherKinds


object RichScoreOps {
    def toRichScore(score: Score) = new RichScoreLike { protected[this] val s = score }
}

trait RichScoreLike {
    protected[this] val s: Score

    import collection.JavaConversions.asScalaBuffer

    /** Get all errors, in DFS preorder.
      * @return
      */
    def allErrors: Seq[ScoreError] = {
        def f(s: List[Score], errors: Vector[ScoreError]): Seq[ScoreError] = s match {
            case Nil => errors
            case h :: t => f(h.getSubScoresList.toList ::: t, if (h.hasError) errors :+ h.getError else errors)
        }
        f(List(s), Vector.empty)
    }

    /** Get all scores in DFS preorder.
      * @return
      */
    def allScores: Seq[Score] = {
        def f(s: List[Score], scores: Vector[Score]): Seq[Score] = s match {
            case Nil => scores
            case h :: t => f(h.getSubScoresList.toList ::: t, scores :+ h)
        }
        f(List(s), Vector.empty)
    }

    def scoreJson: JsValue = {
        val scores = allScores.foldLeft(sci.ListMap.empty[String, JsValue]){(m, s) =>
            if (s.hasScore) {
                val v = s.getScore
                (v.getType: @switch) match {
                    case BOOLEAN => RelaxedConversions.asBoolean(s).fold(m)(x => m + (v.getModel.getId.toString -> JsBoolean(x)))
                    case INT     => RelaxedConversions.asInt(s).fold(m)(x => m + (v.getModel.getId.toString -> JsNumber(x)))
                    case LONG    => RelaxedConversions.asLong(s).fold(m)(x => m + (v.getModel.getId.toString -> JsNumber(x)))
                    case FLOAT   => RelaxedConversions.asFloat(s).fold(m)(x => m + (v.getModel.getId.toString -> JsNumber(x)))
                    case DOUBLE  => RelaxedConversions.asDouble(s).fold(m)(x => m + (v.getModel.getId.toString -> JsNumber(x)))
                    case STRING  => RelaxedConversions.asString(s).fold(m)(x => m + (v.getModel.getId.toString -> JsString(x)))
                    case NONE    => m
                }
            }
            else m
        }
        JsObject(scores)
    }

    def errorJson: JsValue = {
        val errors = allErrors.foldLeft(sci.ListMap.empty[String, JsValue]){(m, e) =>
            val errObj = JsObject(sci.ListMap (
                "errors" -> e.getMessagesList.toJson,
                "missingFeatures" -> e.getMissingFeatures.getNamesList.toJson
            ))

            m + (e.getModel.getId.toString -> errObj)
        }
        JsObject(errors)
    }

    sealed trait ScoreConversions[C[_] <: Option[_]] {
        @inline final def asBoolean = c.asBoolean(s)
        @inline final def asByte = c.asByte(s)
        @inline final def asShort = c.asShort(s)
        @inline final def asInt = c.asInt(s)
        @inline final def asLong = c.asLong(s)
        @inline final def asFloat = c.asFloat(s)
        @inline final def asDouble = c.asDouble(s)
        @inline final def asString = c.asString(s)

        @inline final def asJavaBoolean = c.asJavaBoolean(s)
        @inline final def asJavaByte = c.asJavaByte(s)
        @inline final def asJavaShort = c.asJavaShort(s)
        @inline final def asJavaInteger = c.asJavaInteger(s)
        @inline final def asJavaLong = c.asJavaLong(s)
        @inline final def asJavaFloat = c.asJavaFloat(s)
        @inline final def asJavaDouble = c.asJavaDouble(s)
        @inline final def asJavaString = c.asJavaString(s)

        protected[this] val c: BasicTypeScoreConversions[C]
    }

    /** Provides access the functionality available in [[com.eharmony.aloha.score.conversions.StrictConversions]].
      * See the documentation to understand the implications of using this versus the relaxed accessors.  This is
      * more strict than than relaxed but has a slightly more difficult API.
      */
    //        object strict extends ScoreConversions[StrictType] {
    object strict extends ScoreConversions[({type L[+A]=Option[Either[ScoreType, A]]})#L] {
        final protected[this] val c = StrictConversions
    }

    /** Provides access the functionality available in [[com.eharmony.aloha.score.conversions.RelaxedConversions]].
      * See the documentation to understand the implications of using this versus the strict accessors.  This is
      * more forgiving than strict but looses information.
      */
    object relaxed extends ScoreConversions[Option]{
        final protected[this] val c = RelaxedConversions
    }
}
