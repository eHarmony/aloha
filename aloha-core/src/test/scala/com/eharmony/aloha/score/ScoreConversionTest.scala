package com.eharmony.aloha.score

import com.eharmony.aloha.score.Scores.Score.BaseScore.ScoreType._
import com.eharmony.aloha.score.Scores.Score.{BaseScore, BooleanScore, DoubleScore, FloatScore, IntScore, LongScore, StringScore}
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.BaseModel
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.conversions.ScoreConverter
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.aloha.score.conversions.rich.RichScore
import com.google.protobuf.GeneratedMessage
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/** Test that the implicit conversions work.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ScoreConversionTest {
    import ScoreConversionTest._

    @Test def testBooleanSuccess() {
        val in = BooleanConv.modelId.name
        val o = BooleanConv.score(BooleanConv.modelId.name)
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
        val o = ByteConv.score(ByteConv.modelId.name)
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
        val o = ShortConv.score(ShortConv.modelId.name)
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
        val o = IntConv.score(IntConv.modelId.name)
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
        val o = LongConv.score(LongConv.modelId.name)
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
        val o = FloatConv.score(FloatConv.modelId.name)
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
        val o = DoubleConv.score(DoubleConv.modelId.name)
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
        val o = StringConv.score(StringConv.modelId.name)
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

    /** This test ensures that we get the proper value injected when there are subclasses, each with a ScoreConverter
      * in the implicit scope.
      */
    @Test def testCorrentContainerDuringSubTypeRelation() {
        val xIn = ContainerXConv.modelId.name
        val xO = ContainerXConv.score(ContainerXConv.modelId.name)
        assertNotSame("A score proto should exist.  Should NOT be None.", None, xO)
        val xS: BaseScore = xO.getScore
        assertEquals("Model ID mismatch: ", ContainerXConv.modelId.id, xS.getModel.getId)
        assertEquals("Model name mismatch: ", ContainerXConv.modelId.name, xS.getModel.getName)
        assertEquals("Score should be an int model score: ", STRING, xS.getType)
        assertProperImplOnly(xS, StringScore.impl)
        val xExt = xS.getExtension(StringScore.impl)
        val xSc = xExt.getScore
        assertEquals("model score doesn't match: ", new ContainerX(xIn).toString, xSc)

        val yIn = ContainerYConv.modelId.name
        val yO = ContainerYConv.score(ContainerYConv.modelId.name)
        assertNotSame("A score proto should exist.  Should NOT be None.", None, yO)
        val yS: BaseScore = yO.getScore
        assertEquals("Model ID mismatch: ", ContainerYConv.modelId.id, yS.getModel.getId)
        assertEquals("Model name mismatch: ", ContainerYConv.modelId.name, yS.getModel.getName)
        assertEquals("Score should be an int model score: ", STRING, yS.getType)
        assertProperImplOnly(yS, StringScore.impl)
        val yExt = yS.getExtension(StringScore.impl)
        val ySc = yExt.getScore
        assertEquals("model score doesn't match: ", new ContainerY(yIn).toString, ySc)

        assertTrue(new ContainerY("").isInstanceOf[ContainerY[String]])
        assertTrue(new ContainerY("").isInstanceOf[ContainerX[String]])
    }
}

object ScoreConversionTest {
    type ScoreImpl = GeneratedExtension[BaseScore, _ >: StringScore with DoubleScore with IntScore with FloatScore with LongScore with BooleanScore <: GeneratedMessage]

    private val impls: Set[ScoreImpl] = Set(BooleanScore.impl, IntScore.impl, LongScore.impl, FloatScore.impl, DoubleScore.impl, StringScore.impl)

    def assertProperImplOnly(s: BaseScore, impl: ScoreImpl) {
        assertTrue("Score should be an " + impl, s hasExtension impl)
        (impls - impl).foreach(i => assertFalse("Score should only be an " + impl + ", found " + i, s hasExtension i))
    }


    object BooleanConv extends BaseModel[String, Boolean] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(s.toBoolean) } catch {case e: IllegalArgumentException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(1, "true")
    }

    object ByteConv extends BaseModel[String, Byte] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(s.toByte) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(2, "20")
    }

    object ShortConv extends BaseModel[String, Short] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(s.toShort) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(3, "30")
    }

    object IntConv extends BaseModel[String, Int] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(s.toInt) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(4, "40")
    }

    object LongConv extends BaseModel[String, Long] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(s.toLong) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(5, "50")
    }

    object FloatConv extends BaseModel[String, Float] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(s.toFloat) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(6, "60")
    }

    object DoubleConv extends BaseModel[String, Double] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(s.toDouble) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(7, "70")
    }

    object StringConv extends BaseModel[String, String] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) = success(s)
        val modelId = ModelId(8, "80")
    }

    class ContainerX[+A](value: A) {
        override def toString = "ContainerX(" + value + ")"
    }

    class ContainerY[+A](value: A) extends ContainerX[A](value) {
        override def toString = "ContainerY(" + value + ")"
    }

    object Implicits {
        implicit object ContainerXScoreConverter extends ScoreConverter[ContainerX[String]] {
            val ri = RefInfo[ContainerX[String]]
            val scoreType = STRING
            def boxScore(a: ContainerX[String]) = newBuilder().setExtension(StringScore.impl, StringScore.newBuilder.setScore(a.toString).build)
            def unboxScore(s: BaseScore) = {
                if (STRING == s.getType && s.hasExtension(StringScore.impl)) {
                    val e = s.getExtension(StringScore.impl)
                    if (e.hasScore) Option(new ContainerX(e.getScore)) else None
                }
                else None
            }
        }

        implicit object ContainerYScoreConverter extends ScoreConverter[ContainerY[String]] {
            val ri = RefInfo[ContainerY[String]]
            val scoreType = STRING
            def boxScore(a: ContainerY[String]) = newBuilder().setExtension(StringScore.impl, StringScore.newBuilder.setScore(a.toString).build)
            def unboxScore(s: BaseScore) = {
                if (STRING == s.getType && s.hasExtension(StringScore.impl)) {
                    val e = s.getExtension(StringScore.impl)
                    if (e.hasScore) Option(new ContainerY(e.getScore)) else None
                }
                else None
            }
        }
    }

    import Implicits._

    object ContainerXConv extends BaseModel[String, ContainerX[String]] {
        //         private[aloha] def getScore(s: String)(implicit audit: Boolean) = \/-(s).toScoreTuple

        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(new ContainerX(s)) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(9, "90")
    }

    object ContainerYConv extends BaseModel[String, ContainerY[String]] {
        private[aloha] def getScore(s: String)(implicit audit: Boolean) =
            try { success(new ContainerY(s)) } catch {case e: NumberFormatException => failure(Seq(e.getMessage)) }
        val modelId = ModelId(10, "100")
    }
}
