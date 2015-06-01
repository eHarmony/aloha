package com.eharmony.matching.aloha.score.conversions;

import com.eharmony.matching.aloha.score.Scores;
import com.eharmony.matching.aloha.score.Scores.Score;
import com.eharmony.matching.aloha.score.Scores.Score.ModelId;
import com.eharmony.matching.aloha.score.Scores.Score.BaseScore;
import com.eharmony.matching.aloha.score.Scores.Score.BooleanScore;
import com.eharmony.matching.aloha.score.Scores.Score.IntScore;
import com.eharmony.matching.aloha.score.Scores.Score.LongScore;
import com.eharmony.matching.aloha.score.Scores.Score.FloatScore;
import com.eharmony.matching.aloha.score.Scores.Score.DoubleScore;
import com.eharmony.matching.aloha.score.Scores.Score.StringScore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static com.eharmony.matching.aloha.score.Scores.Score.BaseScore.ScoreType.*;

/**
 * Note there are three atypical cases byte -> short, byte -> int, short -> int.  This is because protocol buffers
 * don't have a type for 1 byte and 2 byte integers.  Therefore, we don't make a special case in the protocol buffers
 * definition of score but rather create separate type classes for short, byte.  These can be up-cast because they are
 * all stored as 32-bit integers.  The rest of the types are properly differentiated.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class StrictConversionsTest {
    static final byte BYTE_SCORE = (byte) 2;
    static final short SHORT_SCORE = (short) 256;
    static final int INT_SCORE = 32768;
    static final long LONG_SCORE = 2;
    static final String STRING_SCORE = "zero";
    static final boolean BOOLEAN_SCORE = true;
    static final float FLOAT_SCORE = 0.5f;
    static final double DOUBLE_SCORE = 0.5;

    @Test public void booleanBoolean() { assertEquals(Boolean.valueOf(BOOLEAN_SCORE), StrictConversions.asJavaBoolean(getBooleanScore())); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void booleanByte() { StrictConversions.asJavaByte(getBooleanScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void booleanShort() { StrictConversions.asJavaShort(getBooleanScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void booleanInteger() { StrictConversions.asJavaInteger(getBooleanScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void booleanLong() { StrictConversions.asJavaLong(getBooleanScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void booleanFloat() { StrictConversions.asJavaFloat(getBooleanScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void booleanDouble() { StrictConversions.asJavaDouble(getBooleanScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void booleanString() { StrictConversions.asJavaString(getBooleanScore()); }

    @Test(expected = IncorrectScoreTypeAccessException.class) public void byteBoolean() { StrictConversions.asJavaBoolean(getByteScore()); }
    @Test public void byteByte() { assertEquals(Byte.valueOf(BYTE_SCORE), StrictConversions.asJavaByte(getByteScore())); }
    /* Special Case */ @Test public void byteShort() { assertEquals(Short.valueOf(BYTE_SCORE), StrictConversions.asJavaShort(getByteScore())); }
    /* Special Case */ @Test public void byteInteger() { assertEquals(Integer.valueOf(BYTE_SCORE), StrictConversions.asJavaInteger(getByteScore())); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void byteLong() { StrictConversions.asJavaLong(getByteScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void byteFloat() { StrictConversions.asJavaFloat(getByteScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void byteDouble() { StrictConversions.asJavaDouble(getByteScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void byteString() { StrictConversions.asJavaString(getByteScore()); }

    @Test(expected = IncorrectScoreTypeAccessException.class) public void shortBoolean() { StrictConversions.asJavaBoolean(getShortScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void shortByte() { StrictConversions.asJavaByte(getShortScore()); }
    @Test public void shortShort() { assertEquals(Short.valueOf(SHORT_SCORE), StrictConversions.asJavaShort(getShortScore())); }
    /* Special Case */ @Test public void shortInteger() { assertEquals(Integer.valueOf(SHORT_SCORE), StrictConversions.asJavaInteger(getShortScore())); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void shortLong() { StrictConversions.asJavaLong(getShortScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void shortFloat() { StrictConversions.asJavaFloat(getShortScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void shortDouble() { StrictConversions.asJavaDouble(getShortScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void shortString() { StrictConversions.asJavaString(getShortScore()); }

    @Test(expected = IncorrectScoreTypeAccessException.class) public void integerBoolean() { StrictConversions.asJavaBoolean(getIntScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void integerByte() { StrictConversions.asJavaByte(getIntScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void integerShort() { StrictConversions.asJavaShort(getIntScore()); }
    @Test public void integerInteger() { assertEquals(Integer.valueOf(INT_SCORE), StrictConversions.asJavaInteger(getIntScore())); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void integerLong() { StrictConversions.asJavaLong(getIntScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void integerFloat() { StrictConversions.asJavaFloat(getIntScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void integerDouble() { StrictConversions.asJavaDouble(getIntScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void integerString() { StrictConversions.asJavaString(getIntScore()); }

    @Test(expected = IncorrectScoreTypeAccessException.class) public void longBoolean() { StrictConversions.asJavaBoolean(getLongScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void longByte() { StrictConversions.asJavaByte(getLongScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void longShort() { StrictConversions.asJavaShort(getLongScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void longInteger() { StrictConversions.asJavaInteger(getLongScore()); }
    @Test public void longLong() { assertEquals(Long.valueOf(LONG_SCORE), StrictConversions.asJavaLong(getLongScore())); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void longFloat() { StrictConversions.asJavaFloat(getLongScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void longDouble() { StrictConversions.asJavaDouble(getLongScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void longString() { StrictConversions.asJavaString(getLongScore()); }

    @Test(expected = IncorrectScoreTypeAccessException.class) public void floatBoolean() { StrictConversions.asJavaBoolean(getFloatScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void floatByte() { StrictConversions.asJavaByte(getFloatScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void floatShort() { StrictConversions.asJavaShort(getFloatScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void floatInteger() { StrictConversions.asJavaInteger(getFloatScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void floatLong() { StrictConversions.asJavaLong(getFloatScore()); }
    @Test public void floatFloat() { assertEquals(FLOAT_SCORE, StrictConversions.asJavaFloat(getFloatScore()), 0); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void floatDouble() { StrictConversions.asJavaDouble(getFloatScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void floatString() { StrictConversions.asJavaString(getFloatScore()); }

    @Test(expected = IncorrectScoreTypeAccessException.class) public void doubleBoolean() { StrictConversions.asJavaBoolean(getDoubleScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void doubleByte() { StrictConversions.asJavaByte(getDoubleScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void doubleShort() { StrictConversions.asJavaShort(getDoubleScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void doubleInteger() { StrictConversions.asJavaInteger(getDoubleScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void doubleLong() { StrictConversions.asJavaLong(getDoubleScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void doubleFloat() { StrictConversions.asJavaFloat(getDoubleScore()); }
    @Test public void doubleDouble() { assertEquals(DOUBLE_SCORE, StrictConversions.asJavaDouble(getDoubleScore()), 0); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void doubleString() { StrictConversions.asJavaString(getDoubleScore()); }

    @Test(expected = IncorrectScoreTypeAccessException.class) public void stringBoolean() { StrictConversions.asJavaBoolean(getStringScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void stringByte() { StrictConversions.asJavaByte(getStringScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void stringShort() { StrictConversions.asJavaShort(getStringScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void stringInteger() { StrictConversions.asJavaInteger(getStringScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void stringLong() { StrictConversions.asJavaLong(getStringScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void stringFloat() { StrictConversions.asJavaFloat(getStringScore()); }
    @Test(expected = IncorrectScoreTypeAccessException.class) public void stringDouble() { StrictConversions.asJavaDouble(getStringScore()); }
    @Test public void stringString() { assertEquals(STRING_SCORE, StrictConversions.asJavaString(getStringScore())); }

    static Score getIntBasedScore(int i) {
        return Scores.Score.newBuilder().setScore(
                BaseScore.newBuilder()
                        .setType(INT)
                        .setExtension(IntScore.impl, IntScore.newBuilder().setScore(i).build())
                        .setModel(ModelId.newBuilder().setId(i).setName(String.valueOf(i)))).build();
    }

    static Score getByteScore() { return getIntBasedScore(BYTE_SCORE); }
    static Score getShortScore() { return getIntBasedScore(SHORT_SCORE); }
    static Score getIntScore() { return getIntBasedScore(INT_SCORE); }

    static Score getLongScore() {
        return Scores.Score.newBuilder().setScore(
                BaseScore.newBuilder()
                        .setType(LONG)
                        .setExtension(LongScore.impl, LongScore.newBuilder().setScore(LONG_SCORE).build())
                        .setModel(ModelId.newBuilder().setId(22).setName(String.valueOf("2L")))).build();
    }

    static Score getFloatScore() {
        return Scores.Score.newBuilder().setScore(
                BaseScore.newBuilder()
                        .setType(FLOAT)
                        .setExtension(FloatScore.impl, FloatScore.newBuilder().setScore(FLOAT_SCORE).build())
                        .setModel(ModelId.newBuilder().setId(12).setName(String.valueOf(FLOAT_SCORE + "f")))).build();
    }

    static Score getDoubleScore() {
        return Scores.Score.newBuilder().setScore(
                BaseScore.newBuilder()
                        .setType(DOUBLE)
                        .setExtension(DoubleScore.impl, DoubleScore.newBuilder().setScore(DOUBLE_SCORE).build())
                        .setModel(ModelId.newBuilder().setId(1212).setName(String.valueOf(DOUBLE_SCORE + "d")))).build();
    }

    static Score getStringScore() {
        return Scores.Score.newBuilder().setScore(
                BaseScore.newBuilder()
                        .setType(STRING)
                        .setExtension(StringScore.impl, StringScore.newBuilder().setScore(STRING_SCORE).build())
                        .setModel(ModelId.newBuilder().setId(0).setName(String.valueOf("0")))).build();
    }

    static Score getBooleanScore() {
        return Scores.Score.newBuilder().setScore(
                BaseScore.newBuilder()
                        .setType(BOOLEAN)
                        .setExtension(BooleanScore.impl, BooleanScore.newBuilder().setScore(BOOLEAN_SCORE).build())
                        .setModel(ModelId.newBuilder().setId(1).setName(String.valueOf(BOOLEAN_SCORE)))).build();
    }
}
