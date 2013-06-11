package com.eharmony.matching.aloha.score.conversions.rich

import scala.language.higherKinds
import com.eharmony.matching.aloha.score.Scores.Score
import com.eharmony.matching.aloha.score.Scores.Score.ScoreError
import com.eharmony.matching.aloha.score.conversions.{RelaxedConversions, StrictConversions, BasicTypeScoreConversions}
import com.eharmony.matching.aloha.score.Scores.Score.BaseScore.ScoreType
import com.eharmony.matching.aloha.score.order.Orderings

object RichScoreOps {
    def toRichScore(score: Score) = new RichScoreLike { protected[this] val s = score }
}

trait RichScoreLike {
    protected[this] val s: Score

    import Orderings._
    import collection.JavaConversions.asScalaBuffer

    /** Get all errors, flattened into an IndexedSeq, sorted by Model ID.
      * @return
      */
    def allErrors: Seq[ScoreError] = {
        def f(s: List[Score], errors: List[ScoreError]): Seq[ScoreError] = s match {
            case Nil => errors.toIndexedSeq.sorted
            case h :: t => f(h.getSubScoresList.toList ::: t, if (h.hasError) h.getError :: errors else errors)
        }
        f(List(s), Nil)
    }

    /** Get all scores, flattened into an IndexedSeq, sorted by Model ID.
      * @return
      */
    def allScores: Seq[Score] = {
        def f(s: List[Score], scores: List[Score]): Seq[Score] = s match {
            case Nil => scores.toIndexedSeq.sorted
            case h :: t => f(h.getSubScoresList.toList ::: t, h :: scores)
        }
        f(List(s), Nil)
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

    /** Provides access the functionality available in [[com.eharmony.matching.aloha.score.conversions.StrictConversions]].
      * See the documentation to understand the implications of using this versus the relaxed accessors.  This is
      * more strict than than relaxed but has a slightly more difficult API.
      */
    //        object strict extends ScoreConversions[StrictType] {
    object strict extends ScoreConversions[({type L[+A]=Option[Either[ScoreType, A]]})#L] {
        final protected[this] val c = StrictConversions
    }

    /** Provides access the functionality available in [[com.eharmony.matching.aloha.score.conversions.RelaxedConversions]].
      * See the documentation to understand the implications of using this versus the strict accessors.  This is
      * more forgiving than strict but looses information.
      */
    object relaxed extends ScoreConversions[Option]{
        final protected[this] val c = RelaxedConversions
    }
}
