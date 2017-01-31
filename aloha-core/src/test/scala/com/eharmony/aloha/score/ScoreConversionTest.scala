package com.eharmony.aloha.score

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.Model
import com.eharmony.aloha.score.Scores.Score
import com.eharmony.aloha.score.Scores.Score.BaseScore.ScoreType._
import com.eharmony.aloha.score.Scores.Score._
import com.eharmony.aloha.audit.impl.scoreproto.ScoreAuditor
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.score.conversions.rich.RichScore
import com.google.protobuf.GeneratedMessage
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.util.Try

/** Test that the implicit conversions work.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ScoreConversionTest {

  import ScoreConversionTest._

  @Test def testBooleanSuccess() {
    val in = BooleanConv.modelId.name
    val o = BooleanConv(BooleanConv.modelId.name)
    assertEquals(BooleanConv.modelId.name.toBoolean, o.relaxed.asBoolean.get)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", BooleanConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", BooleanConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be a boolean model score: ", BOOLEAN, s.getType)
    assertProperImplOnly(s, BooleanScore.impl)
    val ext = s.getExtension(BooleanScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in.toBoolean, sc)
    assertEquals("model score unbox", in.toBoolean, BooleanScoreConverter.unboxScore(s).get)
  }

  @Test def testByteSuccess() {
    val in = ByteConv.modelId.name
    val o = ByteConv(ByteConv.modelId.name)
    assertEquals(ByteConv.modelId.name.toByte, o.relaxed.asByte.get)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", ByteConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", ByteConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be an int model score: ", INT, s.getType)
    assertProperImplOnly(s, IntScore.impl)
    val ext = s.getExtension(IntScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in.toByte, sc)
    assertEquals("model score unbox", in.toByte, ByteScoreConverter.unboxScore(s).get)
  }

  @Test def testShortSuccess() {
    val in = ShortConv.modelId.name
    val o = ShortConv(ShortConv.modelId.name)
    assertEquals(ShortConv.modelId.name.toShort, o.relaxed.asShort.get)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", ShortConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", ShortConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be an int model score: ", INT, s.getType)
    assertProperImplOnly(s, IntScore.impl)
    val ext = s.getExtension(IntScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in.toShort, sc)
    assertEquals("model score unbox", in.toShort, ShortScoreConverter.unboxScore(s).get)
  }

  @Test def testIntSuccess() {
    val in = IntConv.modelId.name
    val o = IntConv(IntConv.modelId.name)
    assertEquals(IntConv.modelId.name.toInt, o.relaxed.asInt.get)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", IntConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", IntConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be an int model score: ", INT, s.getType)
    assertProperImplOnly(s, IntScore.impl)
    val ext = s.getExtension(IntScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in.toInt, sc)
    assertEquals("model score unbox", in.toInt, IntScoreConverter.unboxScore(s).get)
  }

  @Test def testLongSuccess() {
    val in = LongConv.modelId.name
    val o = LongConv(LongConv.modelId.name)
    assertEquals(LongConv.modelId.name.toLong, o.relaxed.asLong.get)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", LongConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", LongConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be an int model score: ", LONG, s.getType)
    assertProperImplOnly(s, LongScore.impl)
    val ext = s.getExtension(LongScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in.toLong, sc)
    assertEquals("model score unbox", in.toLong, LongScoreConverter.unboxScore(s).get)
  }

  @Test def testFloatSuccess() {
    val in = FloatConv.modelId.name
    val o = FloatConv(FloatConv.modelId.name)
    assertEquals(FloatConv.modelId.name.toFloat, o.relaxed.asFloat.get, 0)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", FloatConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", FloatConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be an int model score: ", FLOAT, s.getType)
    assertProperImplOnly(s, FloatScore.impl)
    val ext = s.getExtension(FloatScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in.toFloat, sc, 0)
    assertEquals("model score unbox", in.toFloat, FloatScoreConverter.unboxScore(s).get, 0)
  }

  @Test def testDoubleSuccess() {
    val in = DoubleConv.modelId.name
    val o = DoubleConv(DoubleConv.modelId.name)
    assertEquals(DoubleConv.modelId.name.toDouble, o.relaxed.asDouble.get, 0)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", DoubleConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", DoubleConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be an int model score: ", DOUBLE, s.getType)
    assertProperImplOnly(s, DoubleScore.impl)
    val ext = s.getExtension(DoubleScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in.toDouble, sc, 0)
    assertEquals("model score unbox", in.toDouble, DoubleScoreConverter.unboxScore(s).get, 0)
  }

  @Test def testStringSuccess() {
    val in = StringConv.modelId.name
    val o = StringConv(StringConv.modelId.name)
    assertEquals(StringConv.modelId.name.toString, o.relaxed.asString.get)
    assertNotSame("A score proto should exist.  Should NOT be None.", None, o)
    val s: BaseScore = o.getScore
    assertEquals("Model ID mismatch: ", StringConv.modelId.id, s.getModel.getId)
    assertEquals("Model name mismatch: ", StringConv.modelId.name, s.getModel.getName)
    assertEquals("Score should be an int model score: ", STRING, s.getType)
    assertProperImplOnly(s, StringScore.impl)
    val ext = s.getExtension(StringScore.impl)
    val sc = ext.getScore
    assertEquals("model score doesn't match: ", in, sc)
    assertEquals("model score unbox", in, StringScoreConverter.unboxScore(s).get)
  }
}

object ScoreConversionTest {
  type ScoreImpl = GeneratedExtension[BaseScore, _ >: StringScore with DoubleScore with IntScore with FloatScore with LongScore with BooleanScore <: GeneratedMessage]

  private val impls: Set[ScoreImpl] = Set(BooleanScore.impl, IntScore.impl, LongScore.impl, FloatScore.impl, DoubleScore.impl, StringScore.impl)

  def assertProperImplOnly(s: BaseScore, impl: ScoreImpl) {
    assertTrue("Score should be an " + impl, s hasExtension impl)
    (impls - impl).foreach(i => assertFalse("Score should only be an " + impl + ", found " + i, s hasExtension i))
  }

  object BooleanConv extends Base[Boolean] {
    def modelId = ModelId(1, "true")
    def f: (String) => Boolean = _.toBoolean
  }

  object ByteConv extends Base[Byte] {
    def modelId = ModelId(2, "20")
    def f: (String) => Byte = _.toByte
  }

  object ShortConv extends Base[Short] {
    def modelId = ModelId(3, "30")
    def f: (String) => Short = _.toByte
  }

  object IntConv extends Base[Int] {
    def modelId = ModelId(4, "40")
    def f: (String) => Int = _.toInt
  }

  object LongConv extends Base[Long] {
    def modelId = ModelId(5, "50")
    def f: (String) => Long = _.toLong
  }

  object FloatConv extends Base[Float] {
    def modelId = ModelId(6, "60")
    def f: (String) => Float = _.toFloat
  }

  object DoubleConv extends Base[Double] {
    def modelId = ModelId(7, "70")
    def f: (String) => Double = _.toDouble
  }

  object StringConv extends Base[String] {
    def modelId = ModelId(8, "80")
    def f: (String) => String = identity[String]
  }


  abstract class Base[N](implicit auditor: ScoreAuditor[N]) extends Model[String, Score] {
    def f: String => N
    final override def close(): Unit = {}
    final override def apply(s: String): Score = {
      Try {
        auditor.success(modelId, valueToAudit = f(s))
      }.recover {
        case e => auditor.failure(modelId, Seq(e.getMessage), Set.empty, Nil)
      }.get
    }
  }
}
