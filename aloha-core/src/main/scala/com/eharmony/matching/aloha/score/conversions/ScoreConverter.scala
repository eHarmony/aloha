package com.eharmony.matching.aloha.score.conversions

import java.{lang => jl}

import com.eharmony.aloha.score.Scores.Score.BaseScore.ScoreType
import com.eharmony.aloha.score.Scores.Score.{BaseScore, BooleanScore, DoubleScore, FloatScore, IntScore, LongScore, ModelId, StringScore}
import com.eharmony.matching.aloha.id.ModelIdentity
import com.eharmony.matching.aloha.reflect.RefInfo

import scala.annotation.implicitNotFound

@implicitNotFound(msg = "Cannot find ScoreConverter type class for ${A}.\nConsider including import statement\n\timport com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._")
trait ScoreConverter[A] extends Serializable {
  val ri: RefInfo[A]
  val scoreType: ScoreType
  protected def newBuilder(): BaseScore.Builder = BaseScore.newBuilder.setType(scoreType)

  /**
   * Provides a builder with the score extension already set.
   * @param a a value to box.
   * @return
   */
  def boxScore(a: A): BaseScore.Builder
  def unboxScore(s: BaseScore): Option[A]
}

object ScoreConverter {
  def valueToScore[A](modelId: ModelIdentity, a: A)(implicit c: ScoreConverter[A]): BaseScore =
    c.boxScore(a).setModel(ModelId.newBuilder.setId(modelId.getId()).setName(modelId.getName())).build

  sealed abstract class JavaConverterAdapter[S, J](val ri: RefInfo[J], c: ScoreConverter[S], to: J => S, from: S => J) extends ScoreConverter[J] {
    val scoreType = c.scoreType
    def boxScore(a: J) = c.boxScore(to(a))
    def unboxScore(s: BaseScore) = c.unboxScore(s).map(from)
  }

  object Implicits {
    implicit object JavaBooleanScoreConverter extends JavaConverterAdapter[Boolean, jl.Boolean](RefInfo[jl.Boolean], BooleanScoreConverter, _.booleanValue, jl.Boolean.valueOf _)
    implicit object JavaByteScoreConverter extends JavaConverterAdapter[Byte, jl.Byte](RefInfo[jl.Byte], ByteScoreConverter, _.byteValue, jl.Byte.valueOf _)
    implicit object JavaShortScoreConverter extends JavaConverterAdapter[Short, jl.Short](RefInfo[jl.Short], ShortScoreConverter, _.shortValue, jl.Short.valueOf _)
    implicit object JavaIntScoreConverter extends JavaConverterAdapter[Int, jl.Integer](RefInfo[jl.Integer], IntScoreConverter, _.intValue, jl.Integer.valueOf _)
    implicit object JavaLongScoreConverter extends JavaConverterAdapter[Long, jl.Long](RefInfo[jl.Long], LongScoreConverter, _.longValue, jl.Long.valueOf _)
    implicit object JavaFloatScoreConverter extends JavaConverterAdapter[Float, jl.Float](RefInfo[jl.Float], FloatScoreConverter, _.floatValue, jl.Float.valueOf _)
    implicit object JavaDoubleScoreConverter extends JavaConverterAdapter[Double, jl.Double](RefInfo[jl.Double], DoubleScoreConverter, _.doubleValue, jl.Double.valueOf _)

    implicit object NothingScoreConverter extends ScoreConverter[Nothing] {
      val ri = RefInfo.Nothing
      val scoreType = ScoreType.NONE
      def boxScore(a: Nothing) = newBuilder()
      def unboxScore(s: BaseScore) = None
    }

    implicit object BooleanScoreConverter extends ScoreConverter[Boolean] {
      val ri = RefInfo.Boolean
      val scoreType = ScoreType.BOOLEAN
      def boxScore(a: Boolean) = newBuilder().setExtension(BooleanScore.impl, BooleanScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(BooleanScore.impl)) {
          val e = s.getExtension(BooleanScore.impl)
          if (e.hasScore) Option(e.getScore) else None
        } else None
      }
    }

    implicit object ByteScoreConverter extends ScoreConverter[Byte] {
      val ri = RefInfo.Byte
      val scoreType = ScoreType.INT
      def boxScore(a: Byte) = newBuilder().setExtension(IntScore.impl, IntScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(IntScore.impl)) {
          val e = s.getExtension(IntScore.impl)
          if (e.hasScore) {
            val sc = e.getScore
            if (Byte.MinValue <= sc && sc <= Byte.MaxValue) Option(sc.toByte) else None
          } else None
        } else None
      }
    }

    implicit object ShortScoreConverter extends ScoreConverter[Short] {
      val ri = RefInfo.Short
      val scoreType = ScoreType.INT
      def boxScore(a: Short) = newBuilder().setExtension(IntScore.impl, IntScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(IntScore.impl)) {
          val e = s.getExtension(IntScore.impl)
          if (e.hasScore) {
            val sc = e.getScore
            if (Short.MinValue <= sc && sc <= Short.MaxValue) Option(sc.toShort) else None
          } else None
        } else None
      }
    }

    implicit object IntScoreConverter extends ScoreConverter[Int] {
      val ri = RefInfo.Int
      val scoreType = ScoreType.INT
      def boxScore(a: Int) = newBuilder().setExtension(IntScore.impl, IntScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(IntScore.impl)) {
          val e = s.getExtension(IntScore.impl)
          if (e.hasScore) Option(e.getScore) else None
        } else None
      }
    }

    implicit object LongScoreConverter extends ScoreConverter[Long] {
      val ri = RefInfo.Long
      val scoreType = ScoreType.LONG
      def boxScore(a: Long) = newBuilder().setExtension(LongScore.impl, LongScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(LongScore.impl)) {
          val e = s.getExtension(LongScore.impl)
          if (e.hasScore) Option(e.getScore) else None
        } else None
      }
    }

    implicit object FloatScoreConverter extends ScoreConverter[Float] {
      val ri = RefInfo.Float
      val scoreType = ScoreType.FLOAT
      def boxScore(a: Float) = newBuilder().setExtension(FloatScore.impl, FloatScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(FloatScore.impl)) {
          val e = s.getExtension(FloatScore.impl)
          if (e.hasScore) Option(e.getScore) else None
        } else None
      }
    }

    implicit object DoubleScoreConverter extends ScoreConverter[Double] {
      val ri = RefInfo.Double
      val scoreType = ScoreType.DOUBLE
      def boxScore(a: Double) = newBuilder().setExtension(DoubleScore.impl, DoubleScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(DoubleScore.impl)) {
          val e = s.getExtension(DoubleScore.impl)
          if (e.hasScore) Option(e.getScore) else None
        } else None
      }
    }

    implicit object StringScoreConverter extends ScoreConverter[String] {
      val ri = RefInfo[String]
      val scoreType = ScoreType.STRING
      def boxScore(a: String) = newBuilder().setExtension(StringScore.impl, StringScore.newBuilder.setScore(a).build)
      def unboxScore(s: BaseScore) = {
        if (scoreType == s.getType && s.hasExtension(StringScore.impl)) {
          val e = s.getExtension(StringScore.impl)
          if (e.hasScore) Option(e.getScore) else None
        } else None
      }
    }
  }
}
