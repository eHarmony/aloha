package com.eharmony.aloha.score.conversions

import java.{lang => jl}

import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.Scores.Score.BaseScore.ScoreType
import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._

import scala.language.higherKinds

/** Provide conversions from score to the basic types.
  *
  * @tparam C
  */
sealed trait BasicTypeScoreConversions[C[_] <: Option[_]] extends BasicScalaTypeConversions[C] with BasicTypeJavaScoreConversions {
    /** Get the score as a Boolean wrapped in a container where the outermost container type is Option.  If the Score
      * object doesn't contain a valid score, Return None; otherwise return Some.  Note that subclasses will contain
      * different representations of score.
      * @param s the score
      * @return
      */
    @inline final def asBoolean(s: Score) = conversion[Boolean](s)
    @inline final def asByte(s: Score) = conversion[Byte](s)
    @inline final def asShort(s: Score) = conversion[Short](s)
    @inline final def asInt(s: Score) = conversion[Int](s)
    @inline final def asLong(s: Score) = conversion[Long](s)
    @inline final def asFloat(s: Score) = conversion[Float](s)
    @inline final def asDouble(s: Score) = conversion[Double](s)
    @inline final def asString(s: Score) = conversion[String](s)

    @inline def asJavaBoolean(s: Score) = javaMapper(s, conversion[jl.Boolean](s)).orNull
    @inline def asJavaByte(s: Score) = javaMapper(s, conversion[jl.Byte](s)).orNull
    @inline def asJavaShort(s: Score) = javaMapper(s, conversion[jl.Short](s)).orNull
    @inline def asJavaInteger(s: Score) = javaMapper(s, conversion[jl.Integer](s)).orNull
    @inline def asJavaLong(s: Score) = javaMapper(s, conversion[jl.Long](s)).orNull
    @inline def asJavaFloat(s: Score) = javaMapper(s, conversion[jl.Float](s)).orNull
    @inline def asJavaDouble(s: Score) = javaMapper(s, conversion[jl.Double](s)).orNull
    @inline def asJavaString(s: Score) = javaMapper(s, conversion[String](s)).orNull

    /** Convert a score to a container the score.
      * @param s the score
      * @tparam A the type to which the score should be converted.
      * @return a container containing a value of the expected output type.
      */
    protected[this] def conversion[A: ScoreConverter](s: Score): C[A]

    /**
      * @param s
      * @param container
      * @tparam A
      * @return
      */
    protected[this] def javaMapper[A: RefInfo](s: Score, container: C[A]): Option[A]
}

/** This object DOES NOT differentiate between a missing score and a score that could not be retrieved because the
  * wrong type was queried.  This is harder to deal with than RelaxedOps but encodes additional information.
  * Example usage:
  * {{{
  * import com.eharmony.aloha.score.conversions.RelaxedOps._
  * val asdf: Option[Boolean] = asBoolean(s).map(_.fold(t => throw new Exception("Tried to get a " + t + " as a Boolean"), identity))
  * }}}
  */
object RelaxedConversions extends BasicTypeScoreConversions[Option] {

    /** Get the score.  The Option tells the score exists.  The Right in the embedded Either contains the actual
      * score and the Left contains the type that actually exists in the
      * [[com.eharmony.aloha.score.Scores.Score]] if querying the wrong type.
      * @param s the score
      * @tparam A the type to which the score should be converted.
      * @return
      */
    protected[this] def conversion[A: ScoreConverter](s: Score): Option[A] =
        if (s.hasScore) implicitly[ScoreConverter[A]].unboxScore(s.getScore) else None

    protected[this] def javaMapper[A: RefInfo](s: Score, container: Option[A]) = container
}

/** This object differentiates between a missing score and a score that could not be retrieved because the wrong type
  * was queried.  This is harder to deal with than
  * [[com.eharmony.aloha.score.conversions.RelaxedConversions]] but encodes additional information.
  * Example usage:
  * {{{
  * // Scala
  * import com.eharmony.aloha.io.score.ops.StrictConversions._
  * val asdf: Option[Boolean] = asBoolean(s).map(_.fold(t => throw new Exception("Tried to get a " + t + " as a Boolean"), identity))
  *
  * // Java
  * import static com.eharmony.aloha.score.conversions.StrictConversions.*;
  * // ...
  * Boolean b = null;
  * try {
  *   b = asJavaBoolean(s);
  * } catch (IncorrectScoreTypeAccessException e) {
  *   // ...
  * }
  * }}}
  *
  * Note: If the score value in the score doesn't exist, then the java converters will return null.  If however a score
  * exists but the data type contained in the score is different from the queried type, the java-oriented converters
  * will throw an  [[com.eharmony.aloha.score.conversions.IncorrectScoreTypeAccessException]].
  */
object StrictConversions extends BasicTypeScoreConversions[({type L[+A]=Option[Either[ScoreType, A]]})#L] {

    // NOTE these implementations are defined just for scala doc purposes to get the throws annotation.

    @throws[IncorrectScoreTypeAccessException]("if called on a non-boolean score.")
    @inline override def asJavaBoolean(s: Score) = super.asJavaBoolean(s)

    @throws[IncorrectScoreTypeAccessException]("if called on a non-byte score.")
    @inline override def asJavaByte(s: Score) = super.asJavaByte(s)

    @throws[IncorrectScoreTypeAccessException]("if called on a non-short score.")
    @inline override def asJavaShort(s: Score) = super.asJavaShort(s)

    @throws[IncorrectScoreTypeAccessException]("if called on a non-integer score.")
    @inline override def asJavaInteger(s: Score) = super.asJavaInteger(s)

    @throws[IncorrectScoreTypeAccessException]("if called on a non-long score.")
    @inline override def asJavaLong(s: Score) = super.asJavaLong(s)

    @throws[IncorrectScoreTypeAccessException]("if called on a non-float score.")
    @inline override def asJavaFloat(s: Score) = super.asJavaFloat(s)

    @throws[IncorrectScoreTypeAccessException]("if called on a non-double score.")
    @inline override def asJavaDouble(s: Score) = super.asJavaDouble(s)

    @throws[IncorrectScoreTypeAccessException]("if called on a non-string score.")
    @inline override def asJavaString(s: Score) = super.asJavaString(s)

    protected[this] def conversion[A: ScoreConverter](s: Score) =
        if (s.hasScore) Option(implicitly[ScoreConverter[A]].unboxScore(s.getScore).toRight(s.getScore.getType))
        else None

    /** If no score is present, return None; if a score is present but the wrong type of score was queried, throw an
      * [[com.eharmony.aloha.score.conversions.IncorrectScoreTypeAccessException]].
      * @param s
      * @param container
      * @tparam A
      * @return
      */
    @throws[IncorrectScoreTypeAccessException]("if an attempt is made to access wrong internal data type")
    protected[this] def javaMapper[A: RefInfo](s: Score, container: Option[Either[ScoreType, A]]) =
        container.map(_.fold(_ => throw IncorrectScoreTypeAccessException(s, RefInfoOps.toString(RefInfoOps.refInfo[A])), identity))
}
