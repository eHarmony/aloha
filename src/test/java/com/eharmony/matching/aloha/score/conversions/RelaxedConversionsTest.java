package com.eharmony.matching.aloha.score.conversions;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static com.eharmony.matching.aloha.score.conversions.StrictConversionsTest.*;
import static org.junit.Assert.assertNull;

/**
 * Note there are three atypical cases byte -> short, byte -> int, short -> int.  This is because protocol buffers
 * don't have a type for 1 byte and 2 byte integers.  Therefore, we don't make a special case in the protocol buffers
 * definition of score but rather create separate type classes for short, byte.  These can be up-cast because they are
 * all stored as 32-bit integers.  The rest of the types are properly differentiated.
 */
@RunWith(JUnit4ClassRunner.class)
public class RelaxedConversionsTest {
    @Test public void booleanBoolean() { assertEquals(Boolean.valueOf(BOOLEAN_SCORE), RelaxedConversions.asJavaBoolean(getBooleanScore())); }
    @Test public void booleanByte() { assertNull(RelaxedConversions.asJavaByte(getBooleanScore())); }
    @Test public void booleanShort() { assertNull(RelaxedConversions.asJavaShort(getBooleanScore())); }
    @Test public void booleanInteger() { assertNull(RelaxedConversions.asJavaInteger(getBooleanScore())); }
    @Test public void booleanLong() { assertNull(RelaxedConversions.asJavaLong(getBooleanScore())); }
    @Test public void booleanFloat() { assertNull(RelaxedConversions.asJavaFloat(getBooleanScore())); }
    @Test public void booleanDouble() { assertNull(RelaxedConversions.asJavaDouble(getBooleanScore())); }
    @Test public void booleanString() { assertNull(RelaxedConversions.asJavaString(getBooleanScore())); }

    @Test public void byteBoolean() { assertNull(RelaxedConversions.asJavaBoolean(getByteScore())); }
    @Test public void byteByte() { assertEquals(Byte.valueOf(BYTE_SCORE), RelaxedConversions.asJavaByte(getByteScore())); }
    /* Special Case */ @Test public void byteShort() { assertEquals(Short.valueOf(BYTE_SCORE), RelaxedConversions.asJavaShort(getByteScore())); }
    /* Special Case */ @Test public void byteInteger() { assertEquals(Integer.valueOf(BYTE_SCORE), RelaxedConversions.asJavaInteger(getByteScore())); }
    @Test public void byteLong() { assertNull(RelaxedConversions.asJavaLong(getByteScore())); }
    @Test public void byteFloat() { assertNull(RelaxedConversions.asJavaFloat(getByteScore())); }
    @Test public void byteDouble() { assertNull(RelaxedConversions.asJavaDouble(getByteScore())); }
    @Test public void byteString() { assertNull(RelaxedConversions.asJavaString(getByteScore())); }

    @Test public void shortBoolean() { assertNull(RelaxedConversions.asJavaBoolean(getShortScore())); }
    @Test public void shortByte() { assertNull(RelaxedConversions.asJavaByte(getShortScore())); }
    @Test public void shortShort() { assertEquals(Short.valueOf(SHORT_SCORE), RelaxedConversions.asJavaShort(getShortScore())); }
    /* Special Case */ @Test public void shortInteger() { assertEquals(Integer.valueOf(SHORT_SCORE), RelaxedConversions.asJavaInteger(getShortScore())); }
    @Test public void shortLong() { assertNull(RelaxedConversions.asJavaLong(getShortScore())); }
    @Test public void shortFloat() { assertNull(RelaxedConversions.asJavaFloat(getShortScore())); }
    @Test public void shortDouble() { assertNull(RelaxedConversions.asJavaDouble(getShortScore())); }
    @Test public void shortString() { assertNull(RelaxedConversions.asJavaString(getShortScore())); }

    @Test public void integerBoolean() { assertNull(RelaxedConversions.asJavaBoolean(getIntScore())); }
    @Test public void integerByte() { assertNull(RelaxedConversions.asJavaByte(getIntScore())); }
    @Test public void integerShort() { assertNull(RelaxedConversions.asJavaShort(getIntScore())); }
    @Test public void integerInteger() { assertEquals(Integer.valueOf(INT_SCORE), RelaxedConversions.asJavaInteger(getIntScore())); }
    @Test public void integerLong() { assertNull(RelaxedConversions.asJavaLong(getIntScore())); }
    @Test public void integerFloat() { assertNull(RelaxedConversions.asJavaFloat(getIntScore())); }
    @Test public void integerDouble() { assertNull(RelaxedConversions.asJavaDouble(getIntScore())); }
    @Test public void integerString() { assertNull(RelaxedConversions.asJavaString(getIntScore())); }

    @Test public void longBoolean() { assertNull(RelaxedConversions.asJavaBoolean(getLongScore())); }
    @Test public void longByte() { assertNull(RelaxedConversions.asJavaByte(getLongScore())); }
    @Test public void longShort() { assertNull(RelaxedConversions.asJavaShort(getLongScore())); }
    @Test public void longInteger() { assertNull(RelaxedConversions.asJavaInteger(getLongScore())); }
    @Test public void longLong() { assertEquals(Long.valueOf(LONG_SCORE), RelaxedConversions.asJavaLong(getLongScore())); }
    @Test public void longFloat() { assertNull(RelaxedConversions.asJavaFloat(getLongScore())); }
    @Test public void longDouble() { assertNull(RelaxedConversions.asJavaDouble(getLongScore())); }
    @Test public void longString() { assertNull(RelaxedConversions.asJavaString(getLongScore())); }

    @Test public void floatBoolean() { assertNull(RelaxedConversions.asJavaBoolean(getFloatScore())); }
    @Test public void floatByte() { assertNull(RelaxedConversions.asJavaByte(getFloatScore())); }
    @Test public void floatShort() { assertNull(RelaxedConversions.asJavaShort(getFloatScore())); }
    @Test public void floatInteger() { assertNull(RelaxedConversions.asJavaInteger(getFloatScore())); }
    @Test public void floatLong() { assertNull(RelaxedConversions.asJavaLong(getFloatScore())); }
    @Test public void floatFloat() { assertEquals(FLOAT_SCORE, RelaxedConversions.asJavaFloat(getFloatScore()), 0); }
    @Test public void floatDouble() { assertNull(RelaxedConversions.asJavaDouble(getFloatScore())); }
    @Test public void floatString() { assertNull(RelaxedConversions.asJavaString(getFloatScore())); }

    @Test public void doubleBoolean() { assertNull(RelaxedConversions.asJavaBoolean(getDoubleScore())); }
    @Test public void doubleByte() { assertNull(RelaxedConversions.asJavaByte(getDoubleScore())); }
    @Test public void doubleShort() { assertNull(RelaxedConversions.asJavaShort(getDoubleScore())); }
    @Test public void doubleInteger() { assertNull(RelaxedConversions.asJavaInteger(getDoubleScore())); }
    @Test public void doubleLong() { assertNull(RelaxedConversions.asJavaLong(getDoubleScore())); }
    @Test public void doubleFloat() { assertNull(RelaxedConversions.asJavaFloat(getDoubleScore())); }
    @Test public void doubleDouble() { assertEquals(DOUBLE_SCORE, RelaxedConversions.asJavaDouble(getDoubleScore()), 0); }
    @Test public void doubleString() { assertNull(RelaxedConversions.asJavaString(getDoubleScore())); }

    @Test public void stringBoolean() { assertNull(RelaxedConversions.asJavaBoolean(getStringScore())); }
    @Test public void stringByte() { assertNull(RelaxedConversions.asJavaByte(getStringScore())); }
    @Test public void stringShort() { assertNull(RelaxedConversions.asJavaShort(getStringScore())); }
    @Test public void stringInteger() { assertNull(RelaxedConversions.asJavaInteger(getStringScore())); }
    @Test public void stringLong() { assertNull(RelaxedConversions.asJavaLong(getStringScore())); }
    @Test public void stringFloat() { assertNull(RelaxedConversions.asJavaFloat(getStringScore())); }
    @Test public void stringDouble() { assertNull(RelaxedConversions.asJavaDouble(getStringScore())); }
    @Test public void stringString() { assertEquals(STRING_SCORE, RelaxedConversions.asJavaString(getStringScore())); }
}
