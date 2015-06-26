package com.eharmony.aloha.semantics.compiled.plugin.csv

import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{Ignore, Test}
import org.junit.Assert._

import scalaz.syntax.std.list.ToListOpsFromList
import scala.collection.immutable.IndexedSeq

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvLineTest {
    import CsvLineTest._

    private[this] implicit val rand = new util.Random(0)

    // =====================  NUMERIC 0 PARSING  =====================

    @Test def testReqIntZero() {
        assertEquals(0, extract("required.int", "0", _.i))
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntZeroLlc() {
        extract("required.int", "0l", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntZeroLuc() {
        extract("required.int", "0L", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntZeroFlc() {
        extract("required.int", "0f", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntZeroFuc() {
        extract("required.int", "0F", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntZeroDlc() {
        extract("required.int", "0d", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntZeroDuc() {
        extract("required.int", "0D", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntZeroPointZero() {
        extract("required.int", "0.0", _.i)
    }

    @Test def testReqLongZero() {
        assertEquals(0, extract("required.long", "0", _.l))
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongZeroLlc() {
        extract("required.long", "0l", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongZeroLuc() {
        extract("required.long", "0L", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongZeroFlc() {
        extract("required.long", "0f", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongZeroFuc() {
        extract("required.long", "0F", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongZeroDlc() {
        extract("required.long", "0d", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongZeroDuc() {
        extract("required.long", "0D", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongZeroPointZero() {
        extract("required.long", "0.0", _.l)
    }

    @Test def testReqFloatZero() {
        assertEquals(0, extract("required.float", "0", _.f), 0)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqFloatZeroLlc() {
        extract("required.float", "0l", _.f)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqFloatZeroLuc() {
        extract("required.float", "0L", _.f)
    }

    @Test def testReqFloatZeroFlc() {
        assertEquals(0, extract("required.float", "0f", _.f), 0)
    }

    @Test def testReqFloatZeroFuc() {
        assertEquals(0, extract("required.float", "0F", _.f), 0)
    }

    @Test def testReqFloatZeroDlc() {
        assertEquals(0, extract("required.float", "0d", _.f), 0)
    }

    @Test def testReqFloatZeroDuc() {
        assertEquals(0, extract("required.float", "0D", _.f), 0)
    }

    @Test def testReqFloatZeroPointZero() {
        assertEquals(0, extract("required.float", "0.0", _.f), 0)
    }

    @Test def testReqDoubleZero() {
        assertEquals(0, extract("required.double", "0", _.d), 0)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqDoubleZeroLlc() {
        extract("required.double", "0l", _.d)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqDoubleZeroLuc() {
        extract("required.double", "0L", _.d)
    }

    @Test def testReqDoubleZeroFlc() {
        assertEquals(0, extract("required.double", "0f", _.d), 0)
    }

    @Test def testReqDoubleZeroFuc() {
        assertEquals(0, extract("required.double", "0F", _.d), 0)
    }

    @Test def testReqDoubleZeroDlc() {
        assertEquals(0, extract("required.double", "0d", _.d), 0)
    }

    @Test def testReqDoubleZeroDuc() {
        assertEquals(0, extract("required.double", "0D", _.d), 0)
    }

    @Test def testReqDoubleZeroPointZero() {
        assertEquals(0, extract("required.double", "0.0", _.d), 0)
    }

    // =====================  NUMERIC 0 PARSING  =====================

    @Test(expected = classOf[IllegalArgumentException])
    def testReqEnumEmptyString() {
        extract("required.enum", "", _.e)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testReqEnumBadValue() {
        extract("required.enum", "NON_EXISTENT_CONSTANT", _.e)
    }

    @Test def testReqEnumExists() {
        val tests = List("MALE", "FEMALE")
        tests foreach { k => assertEquals(GenderProto valueOf k, extract("required.enum", k, _.e)) }
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testReqBoolEmptyString() {
        extract("required.boolean", "", _.b)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testReqBoolBadValue() {
        extract("required.boolean", "NOT_TRUE", _.b)
    }

    @Test def testReqBoolTrueUc() {
        assertEquals(true, extract("required.boolean", "True", _.b))
    }

    @Test def testReqBoolTrueLc() {
        assertEquals(true, extract("required.boolean", "true", _.b))
    }

    @Test def testReqBoolTrueAllCaps() {
        assertEquals(true, extract("required.boolean", "TRUE", _.b))
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntEmptyString() {
        extract("required.int", "", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntBadValueBig() {
        extract("required.int", "2147483648", _.i)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqIntValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        extract("required.int", hex32767Literal, _.i)
    }

    @Test def testReqIntValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("required.int", oct32767Literal, _.i)
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test
    def testReqIntValid() {
        val tests = Seq.fill(1000)(rand.nextInt())
        tests foreach { i => assertEquals(s"For int: $i", i, extract("required.int", i.toString, _.i)) }
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongEmptyString() {
        extract("required.long", "", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongBadValueFloat() {
        extract("required.long", "0f", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongBadValueDouble() {
        extract("required.long", "0.0", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongBadValueBig() {
        extract("required.long", "123456789012345678901234567890l", _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        extract("required.long", hex32767Literal, _.l)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongValidHexL() {
        val hex32767Literal = "0x7fffl" // 32767
        extract("required.long", hex32767Literal, _.l)
    }

    @Test def testReqLongValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("required.long", oct32767Literal, _.l)
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqLongValidOctL() {
        val oct32767Literal = "077777l" // 32767
        val oct = extract("required.long", oct32767Literal, _.l)
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test
    def testReqLongValid() {
        val tests = Seq.fill(1000)(rand.nextLong())
        tests foreach { i => assertEquals(s"For long: $i", i, extract("required.long", i.toString, _.l)) }
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqFloatEmptyString() {
        extract("required.float", "", _.f)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqFloatBadValueLong() {
        extract("required.float", "1l", _.f)
    }

    @Test def testReqFloatAsDouble() {
        assertEquals(0f, extract("required.float", "0.0", _.f), 0)
    }

    @Test def testReqFloatBadValueBig() {
        assertEquals(
            Float.PositiveInfinity,
            extract("required.float", "1234567890123456789012345678901234567890f", _.f),
            0
        )
    }

    @Test def testReqFloatBadValueBigOk() {
        assertEquals(
            123456789012345678901234567890123456789f,
            extract("required.float", "123456789012345678901234567890123456789f", _.f),
            0
        )
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqFloatValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        extract("required.float", hex32767Literal, _.f)
    }

    @Test def testReqFloatValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("required.float", oct32767Literal, _.f)
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct, 0)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test
    def testReqFloatValid() {
        val tests = Seq.fill(1000)(rand.nextFloat())
        tests foreach { i => assertEquals(s"For float: $i", i, extract("required.float", s"${i}f", _.f), 0) }
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqDoubleEmptyString() {
        extract("required.double", "", _.d)
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqDoubleBadValueLong() {
        extract("required.double", "1l", _.d)
    }

    @Test def testReqDoubleBadValueDouble() {
        assertEquals(0, extract("required.double", "0f", _.d), 0)
    }

    @Test def testReqDoubleBadValueBig() {
        assertEquals(
            Double.PositiveInfinity,
            extract("required.double", "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890d", _.d),
            0
        )
    }

    @Test def testReqDoubleBadValueBigOk() {
        assertEquals(
            123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d,
            extract("required.double", "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d", _.d),
            0
        )
    }

    @Test(expected = classOf[NumberFormatException])
    def testReqDoubleValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        extract("required.double", hex32767Literal, _.d)
    }

    @Test def testReqDoubleValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("required.double", oct32767Literal, _.d)
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct, 0)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test def testReqDoubleValid() {
        val tests = Seq.fill(1000)(rand.nextDouble())
        tests foreach { i => assertEquals(s"For double: $i", i, extract("required.double", i.toString, _.d), 0) }
    }

    @Test def testReqEmptyString() {
        assertEquals(s"For string: ''", "", extract("required.string", "", _.s))
    }

    @Test def testReqStrings() {
        val tests = Seq.fill(1000)(BigInt(rand.nextString(100).getBytes).toString(32))
        tests foreach { s => assertEquals(s"For string: '$s'", s, extract("required.string", s, _.s)) }
    }


    // TODO: Optional values

    // =====================  NUMERIC 0 PARSING  =====================

    @Test def testOptIntZero(): Unit             = assertEquals(0,    extract("optional.int", "0", _.oi).get)
    @Test def testOptIntZeroLlc(): Unit          = assertEquals(None, extract("optional.int", "0l", _.oi))
    @Test def testOptIntZeroLuc(): Unit          = assertEquals(None, extract("optional.int", "0L", _.oi))
    @Test def testOptIntZeroFlc(): Unit          = assertEquals(None, extract("optional.int", "0f", _.oi))
    @Test def testOptIntZeroFuc(): Unit          = assertEquals(None, extract("optional.int", "0F", _.oi))
    @Test def testOptIntZeroDlc(): Unit          = assertEquals(None, extract("optional.int", "0d", _.oi))
    @Test def testOptIntZeroDuc(): Unit          = assertEquals(None, extract("optional.int", "0D", _.oi))
    @Test def testOptIntZeroPointZero(): Unit    = assertEquals(None, extract("optional.int", "0.0", _.oi))

    @Test def testOptLongZero(): Unit            = assertEquals(0,    extract("optional.long", "0", _.ol).get)
    @Test def testOptLongZeroLlc(): Unit         = assertEquals(None, extract("optional.long", "0l", _.ol))
    @Test def testOptLongZeroLuc(): Unit         = assertEquals(None, extract("optional.long", "0L", _.ol))
    @Test def testOptLongZeroFlc(): Unit         = assertEquals(None, extract("optional.long", "0f", _.ol))
    @Test def testOptLongZeroFuc(): Unit         = assertEquals(None, extract("optional.long", "0F", _.ol))
    @Test def testOptLongZeroDlc(): Unit         = assertEquals(None, extract("optional.long", "0d", _.ol))
    @Test def testOptLongZeroDuc(): Unit         = assertEquals(None, extract("optional.long", "0D", _.ol))
    @Test def testOptLongZeroPointZero(): Unit   = assertEquals(None, extract("optional.long", "0.0", _.ol))

    @Test def testOptFloatZero(): Unit           = assertEquals(0,    extract("optional.float", "0", _.of).get, 0)
    @Test def testOptFloatZeroLlc(): Unit        = assertEquals(None, extract("optional.float", "0l", _.of))
    @Test def testOptFloatZeroLuc(): Unit        = assertEquals(None, extract("optional.float", "0L", _.of))
    @Test def testOptFloatZeroFlc(): Unit        = assertEquals(0,    extract("optional.float", "0f", _.of).get, 0)
    @Test def testOptFloatZeroFuc(): Unit        = assertEquals(0,    extract("optional.float", "0F", _.of).get, 0)
    @Test def testOptFloatZeroDlc(): Unit        = assertEquals(0,    extract("optional.float", "0d", _.of).get, 0)
    @Test def testOptFloatZeroDuc(): Unit        = assertEquals(0,    extract("optional.float", "0D", _.of).get, 0)
    @Test def testOptFloatZeroPointZero(): Unit  = assertEquals(0,    extract("optional.float", "0.0", _.of).get, 0)

    @Test def testOptDoubleZero(): Unit          = assertEquals(0,    extract("optional.double", "0", _.od).get, 0)
    @Test def testOptDoubleZeroLlc(): Unit       = assertEquals(None, extract("optional.double", "0l", _.od))
    @Test def testOptDoubleZeroLuc(): Unit       = assertEquals(None, extract("optional.double", "0L", _.od))
    @Test def testOptDoubleZeroFlc(): Unit       = assertEquals(0,    extract("optional.double", "0f", _.od).get, 0)
    @Test def testOptDoubleZeroFuc(): Unit       = assertEquals(0,    extract("optional.double", "0F", _.od).get, 0)
    @Test def testOptDoubleZeroDlc(): Unit       = assertEquals(0,    extract("optional.double", "0d", _.od).get, 0)
    @Test def testOptDoubleZeroDuc(): Unit       = assertEquals(0,    extract("optional.double", "0D", _.od).get, 0)
    @Test def testOptDoubleZeroPointZero(): Unit = assertEquals(0,    extract("optional.double", "0.0", _.od).get, 0)

    // =====================  NUMERIC 0 PARSING  =====================

    @Test def testOptEnumEmptyString() {
        assertEquals(None, extract("optional.enum", "", _.oe))
    }

    @Test def testOptEnumBadValue() {
        assertEquals(None, extract("optional.enum", "NON_EXISTENT_CONSTANT", _.oe))
    }

    @Test def testOptEnumExists() {
        val tests = List("MALE", "FEMALE")
        tests foreach { k => assertEquals(Option(GenderProto.valueOf(k)), extract("optional.enum", k, _.oe)) }
    }

    @Test def testOptBoolEmptyString() {
        assertEquals(None, extract("optional.boolean", "", _.ob))
    }

    @Test def testOptBoolBadValue() {
        assertEquals(None, extract("optional.boolean", "NOT_TRUE", _.ob))
    }

    @Test def testOptBoolTrueUc() {
        assertEquals(Some(true), extract("optional.boolean", "True", _.ob))
    }

    @Test def testOptBoolTrueLc() {
        assertEquals(Some(true), extract("optional.boolean", "true", _.ob))
    }

    @Test def testOptBoolTrueAllCaps() {
        assertEquals(Some(true), extract("optional.boolean", "TRUE", _.ob))
    }

    @Test def testOptIntEmptyString() {
        assertEquals(None, extract("optional.int", "", _.oi))
    }

    @Test def testOptIntBadValueFloat() {
        assertEquals(None, extract("optional.int", "0f", _.oi))
    }

    @Test def testOptIntBadValueDouble() {
        assertEquals(None, extract("optional.int", "0.0", _.oi))
    }

    @Test def testOptIntBadValueLong() {
        assertEquals(None, extract("optional.int", "1l", _.oi))
    }

    @Test def testOptIntBadValueBig() {
        assertEquals(None, extract("optional.int", "2147483648", _.oi))
    }

    @Test def testOptIntValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        assertEquals(None, extract("optional.int", hex32767Literal, _.oi))
    }

    @Test def testOptIntValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("optional.int", oct32767Literal, _.oi).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test
    def testOptIntValid() {
        val tests = Seq.fill(1000)(rand.nextInt())
        tests foreach { i => assertEquals(s"For int: $i", i, extract("optional.int", i.toString, _.oi).get) }
    }

    @Test def testOptLongEmptyString() {
        assertEquals(None, extract("optional.long", "", _.ol))
    }

    @Test def testOptLongBadValueFloat() {
        assertEquals(None, extract("optional.long", "0f", _.ol))
    }

    @Test def testOptLongBadValueDouble() {
        assertEquals(None, extract("optional.long", "0.0", _.ol))
    }

    @Test def testOptLongBadValueBig() {
        assertEquals(None, extract("optional.long", "123456789012345678901234567890l", _.ol))
    }

    @Test def testOptLongValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        assertEquals(None, extract("optional.long", hex32767Literal, _.ol))
    }

    @Test def testOptLongValidHexL() {
        val hex32767Literal = "0x7fffl" // 32767
        assertEquals(None, extract("optional.long", hex32767Literal, _.ol))
    }

    @Test def testOptLongValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("optional.long", oct32767Literal, _.ol).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test def testOptLongValidOctL() {
        val oct32767Literal = "077777l" // 32767
        assertEquals(None, extract("optional.long", oct32767Literal, _.ol))
    }

    @Test def testOptLongValid() {
        val tests = Seq.fill(1000)(rand.nextLong())
        tests foreach { i => assertEquals(s"For long: $i", i, extract("optional.long", i.toString, _.ol).get) }
    }

    @Test def testOptFloatEmptyString() {
        assertEquals(None, extract("optional.float", "", _.of))
    }

    @Test def testOptFloatBadValueLong() {
        assertEquals(None, extract("optional.float", "1l", _.of))
    }

    @Test def testOptFloatAsDouble() {
        assertEquals(0f, extract("optional.float", "0.0", _.of).get, 0)
    }

    @Test def testOptFloatBadValueBig() {
        assertEquals(
            Float.PositiveInfinity,
            extract("optional.float", "1234567890123456789012345678901234567890f", _.of).get,
            0
        )
    }

    @Test def testOptFloatBadValueBigOk() {
        assertEquals(
            123456789012345678901234567890123456789f,
            extract("optional.float", "123456789012345678901234567890123456789f", _.of).get,
            0
        )
    }

    @Test def testOptFloatValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        assertEquals(None, extract("required.float", hex32767Literal, _.of))
    }

    @Test def testOptFloatValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("required.float", oct32767Literal, _.of).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct, 0)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test
    def testOptFloatValid() {
        val tests = Seq.fill(1000)(rand.nextFloat())
        tests foreach { i => assertEquals(s"For float: $i", i, extract("optional.float", s"${i}f", _.of).get, 0f) }
    }

    @Test def testOptDoubleEmptyString() {
        assertEquals(None, extract("optional.double", "", _.od))
    }

    @Test def testOptDoubleBadValueLong() {
        assertEquals(None, extract("optional.double", "1l", _.od))
    }

    @Test def testOptDoubleBadValueDouble() {
        assertEquals(0, extract("optional.double", "0f", _.od).getOrElse(Double.NaN), 0)
    }

    @Test def testOptDoubleBadValueBig() {
        assertEquals(
            Double.PositiveInfinity,
            extract("optional.double", "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890d", _.od).get,
            0
        )
    }

    @Test def testOptDoubleBadValueBigOk() {
        assertEquals(
            123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d,
            extract("optional.double", "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d", _.od).get,
            0
        )
    }

    @Test def testOptDoubleValidHex() {
        val hex32767Literal = "0x7fff" // 32767
        assertEquals(None, extract("required.double", hex32767Literal, _.od))
    }

    @Test def testOptDoubleValidOct() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("required.double", oct32767Literal, _.od).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct, 0)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test def testOptDoubleValid() {
        val tests = Seq.fill(1000)(rand.nextDouble())
        tests foreach { i => assertEquals(s"For double: $i", i, extract("optional.double", i.toString, _.od).get, 0) }
    }

    @Test def testOptEmptyString() {
        assertEquals(s"For string: ''", Option(""), extract("optional.string", "", _.os))
    }

    @Test def testOptStrings() {
        val tests = Seq.fill(1000)(BigInt(rand.nextString(100).getBytes).toString(32))
        tests foreach { s => assertEquals(s"For string: '$s'", Option(s), extract("optional.string", s, _.os)) }
    }


    // TODO: Optional Mean Functions


    // =====================  NUMERIC 0 PARSING  =====================

    @Test def testOptIntZeroMean(): Unit = assertEquals(0, extractMean("optional.int", "0", _.oi).get)
    @Test(expected = classOf[NumberFormatException]) def testOptIntZeroLlcMean(): Unit = extractMean("optional.int", "0l", _.oi)
    @Test(expected = classOf[NumberFormatException]) def testOptIntZeroLucMean(): Unit = extractMean("optional.int", "0L", _.oi)
    @Test(expected = classOf[NumberFormatException]) def testOptIntZeroFlcMean(): Unit = extractMean("optional.int", "0f", _.oi)
    @Test(expected = classOf[NumberFormatException]) def testOptIntZeroFucMean(): Unit = extractMean("optional.int", "0F", _.oi)
    @Test(expected = classOf[NumberFormatException]) def testOptIntZeroDlcMean(): Unit = extractMean("optional.int", "0d", _.oi)
    @Test(expected = classOf[NumberFormatException]) def testOptIntZeroDucMean(): Unit = extractMean("optional.int", "0D", _.oi)
    @Test(expected = classOf[NumberFormatException]) def testOptIntZeroPointZeroMean(): Unit = extractMean("optional.int", "0.0", _.oi)

    @Test def testOptLongZeroMean(): Unit = assertEquals(0, extractMean("optional.long", "0", _.ol).get)
    @Test(expected = classOf[NumberFormatException]) def testOptLongZeroLlcMean(): Unit = extractMean("optional.long", "0l", _.ol)
    @Test(expected = classOf[NumberFormatException]) def testOptLongZeroLucMean(): Unit = extractMean("optional.long", "0L", _.ol)
    @Test(expected = classOf[NumberFormatException]) def testOptLongZeroFlcMean(): Unit = extractMean("optional.long", "0f", _.ol)
    @Test(expected = classOf[NumberFormatException]) def testOptLongZeroFucMean(): Unit = extractMean("optional.long", "0F", _.ol)
    @Test(expected = classOf[NumberFormatException]) def testOptLongZeroDlcMean(): Unit = extractMean("optional.long", "0d", _.ol)
    @Test(expected = classOf[NumberFormatException]) def testOptLongZeroDucMean(): Unit = extractMean("optional.long", "0D", _.ol)
    @Test(expected = classOf[NumberFormatException]) def testOptLongZeroPointZeroMean(): Unit = extractMean("optional.long", "0.0", _.ol)

    @Test def testOptFloatZeroMean(): Unit           = assertEquals(0,    extractMean("optional.float", "0", _.of).get, 0)
    @Test(expected = classOf[NumberFormatException]) def testOptFloatZeroLlcMean(): Unit = extractMean("optional.float", "0l", _.of)
    @Test(expected = classOf[NumberFormatException]) def testOptFloatZeroLucMean(): Unit = extractMean("optional.float", "0L", _.of)
    @Test def testOptFloatZeroFlcMean(): Unit        = assertEquals(0,    extractMean("optional.float", "0f", _.of).get, 0)
    @Test def testOptFloatZeroFucMean(): Unit        = assertEquals(0,    extractMean("optional.float", "0F", _.of).get, 0)
    @Test def testOptFloatZeroDlcMean(): Unit        = assertEquals(0,    extractMean("optional.float", "0d", _.of).get, 0)
    @Test def testOptFloatZeroDucMean(): Unit        = assertEquals(0,    extractMean("optional.float", "0D", _.of).get, 0)
    @Test def testOptFloatZeroPointZeroMean(): Unit  = assertEquals(0,    extractMean("optional.float", "0.0", _.of).get, 0)

    @Test def testOptDoubleZeroMean(): Unit          = assertEquals(0,    extractMean("optional.double", "0", _.od).get, 0)
    @Test(expected = classOf[NumberFormatException]) def testOptDoubleZeroLlcMean(): Unit = extractMean("optional.double", "0l", _.od)
    @Test(expected = classOf[NumberFormatException]) def testOptDoubleZeroLucMean(): Unit = extractMean("optional.double", "0L", _.od)
    @Test def testOptDoubleZeroFlcMean(): Unit       = assertEquals(0,    extractMean("optional.double", "0f", _.od).get, 0)
    @Test def testOptDoubleZeroFucMean(): Unit       = assertEquals(0,    extractMean("optional.double", "0F", _.od).get, 0)
    @Test def testOptDoubleZeroDlcMean(): Unit       = assertEquals(0,    extractMean("optional.double", "0d", _.od).get, 0)
    @Test def testOptDoubleZeroDucMean(): Unit       = assertEquals(0,    extractMean("optional.double", "0D", _.od).get, 0)
    @Test def testOptDoubleZeroPointZeroMean(): Unit = assertEquals(0,    extractMean("optional.double", "0.0", _.od).get, 0)

    // =====================  NUMERIC 0 PARSING  =====================

    @Test(expected = classOf[IllegalArgumentException])
    def testOptEnumEmptyStringMean() {
        extractMean("optional.enum", "", _.oe)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testOptEnumBadValueMean() {
        extractMean("optional.enum", "NON_EXISTENT_CONSTANT", _.oe)
    }

    @Test def testOptEnumExistsMean() {
        val tests = List("MALE", "FEMALE")
        tests foreach { k => assertEquals(Option(GenderProto.valueOf(k)), extractMean("optional.enum", k, _.oe)) }
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testOptBoolEmptyStringMean() {
        extractMean("optional.boolean", "", _.ob)
    }

    @Test(expected = classOf[IllegalArgumentException]) def testOptBoolBadValueMean() {
        extractMean("optional.boolean", "NOT_TRUE", _.ob)
    }

    @Test def testOptBoolTrueUcMean() {
        assertEquals(Some(true), extractMean("optional.boolean", "True", _.ob))
    }

    @Test def testOptBoolTrueLcMean() {
        assertEquals(Some(true), extractMean("optional.boolean", "true", _.ob))
    }

    @Test def testOptBoolTrueAllCapsMean() {
        assertEquals(Some(true), extractMean("optional.boolean", "TRUE", _.ob))
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptIntEmptyStringMean() {
        extractMean("optional.int", "", _.oi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptIntBadValueFloatMean() {
        extractMean("optional.int", "0f", _.oi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptIntBadValueDoubleMean() {
        extractMean("optional.int", "0.0", _.oi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptIntBadValueLongMean() {
        extractMean("optional.int", "1l", _.oi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptIntBadValueBigMean() {
        extractMean("optional.int", "2147483648", _.oi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptIntValidHexMean() {
        val hex32767Literal = "0x7fff" // 32767
        assertEquals(None, extractMean("optional.int", hex32767Literal, _.oi))
    }

    @Test def testOptIntValidOctMean() {
        val oct32767Literal = "077777" // 32767
        val oct = extractMean("optional.int", oct32767Literal, _.oi).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test def testOptIntValidMean() {
        val tests = Seq.fill(1000)(rand.nextInt())
        tests foreach { i => assertEquals(s"For int: $i", i, extractMean("optional.int", i.toString, _.oi).get) }
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptLongEmptyStringMean() {
        extractMean("optional.long", "", _.ol)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptLongBadValueFloatMean() {
        extractMean("optional.long", "0f", _.ol)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptLongBadValueDoubleMean() {
        extractMean("optional.long", "0.0", _.ol)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptLongBadValueBigMean() {
        extractMean("optional.long", "123456789012345678901234567890l", _.ol)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptLongValidHexMean() {
        val hex32767Literal = "0x7fff" // 32767
        extractMean("optional.long", hex32767Literal, _.ol)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptLongValidHexLMean() {
        val hex32767Literal = "0x7fffl" // 32767
        extractMean("optional.long", hex32767Literal, _.ol)
    }

    @Test def testOptLongValidOctMean() {
        val oct32767Literal = "077777" // 32767
        val oct = extractMean("optional.long", oct32767Literal, _.ol).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptLongValidOctLMean() {
        val oct32767Literal = "077777l" // 32767
        val oct = extractMean("optional.long", oct32767Literal, _.ol).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test def testOptLongValidMean() {
        val tests = Seq.fill(1000)(rand.nextLong())
        tests foreach { i => assertEquals(s"For long: $i", i, extractMean("optional.long", i.toString, _.ol).get) }
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptFloatEmptyStringMean() {
        extractMean("optional.float", "", _.of)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptFloatBadValueLongMean() {
        extractMean("optional.float", "1l", _.of)
    }

    @Test def testOptFloatAsDoubleMean() {
        assertEquals(0f, extractMean("optional.float", "0.0", _.of).get, 0)
    }

    @Test def testOptFloatBadValueBigMean() {
        assertEquals(
            Float.PositiveInfinity,
            extractMean("optional.float", "1234567890123456789012345678901234567890f", _.of).get,
            0
        )
    }

    @Test def testOptFloatBadValueBigOkMean() {
        assertEquals(
            123456789012345678901234567890123456789f,
            extractMean("optional.float", "123456789012345678901234567890123456789f", _.of).get,
            0
        )
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptFloatValidHexMean() {
        val hex32767Literal = "0x7fff" // 32767
        extractMean("optional.float", hex32767Literal, _.of)
    }

    @Test def testOptFloatValidOctMean() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("optional.float", oct32767Literal, _.of).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct, 0)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test def testOptFloatValidMean() {
        val tests = Seq.fill(1000)(rand.nextFloat())
        tests foreach { i => assertEquals(s"For float: $i", i, extractMean("optional.float", s"${i}f", _.of).get, 0f) }
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptDoubleEmptyStringMean() {
        extractMean("optional.double", "", _.od)
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptDoubleBadValueLongMean() {
        extractMean("optional.double", "1l", _.od)
    }

    @Test def testOptDoubleBadValueDoubleMean() {
        assertEquals(0, extractMean("optional.double", "0f", _.od).getOrElse(Double.NaN), 0)
    }

    @Test def testOptDoubleBadValueBigMean() {
        assertEquals(
            Double.PositiveInfinity,
            extractMean("optional.double", "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890d", _.od).get,
            0
        )
    }

    @Test def testOptDoubleBadValueBigOkMean() {
        assertEquals(
            123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d,
            extractMean("optional.double", "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d", _.od).get,
            0
        )
    }

    @Test(expected = classOf[NumberFormatException])
    def testOptDoubleValidHexMean() {
        val hex32767Literal = "0x7fff" // 32767
        extractMean("optional.double", hex32767Literal, _.od)
    }

    @Test def testOptDoubleValidOctMean() {
        val oct32767Literal = "077777" // 32767
        val oct = extract("optional.double", oct32767Literal, _.od).get
        assertEquals("Octal literal should parse as decimal literal.", 77777, oct, 0)
        assertFalse("Octal literal should parse as decimal literal.", 32767 == oct)
    }

    @Test def testOptDoubleValidMean() {
        val tests = Seq.fill(1000)(rand.nextDouble())
        tests foreach { i => assertEquals(s"For double: $i", i, extractMean("optional.double", i.toString, _.od).get, 0) }
    }

    @Test def testOptEmptyStringMean() {
        assertEquals(s"For string: ''", Option(""), extractMean("optional.string", "", _.os))
    }

    @Test def testOptStringsMean() {
        val tests = Seq.fill(1000)(BigInt(rand.nextString(100).getBytes).toString(32))
        tests foreach { s => assertEquals(s"For string: '$s'", Option(s), extractMean("optional.string", s, _.os)) }
    }



    // TODO: Vector Functions

    @Test def testRepeatedEnumEmptyString() {
        assertEquals(Vector.empty, extract("repeated.enum", "", _.ve))
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testRepeatedEnumBadValue() {
        extract("repeated.enum", "NON_EXISTENT_CONSTANT", _.ve)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testRepeatedEnumBadLeftValue() {
        extract("repeated.enum", "NON_EXISTENT_CONSTANT,ACCEPTED", _.ve)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testRepeatedEnumBadRightValue() {
        extract("repeated.enum", "ACCEPTED,NON_EXISTENT_CONSTANT", _.ve)
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testRepeatedEnumBadRightValueSpace() {
        extract("repeated.enum", "ACCEPTED, UPLOADED", _.ve)
    }

    @Test def testRepeatedEnumExists() {
        val tests = List("ACCEPTED", "UPLOADED", "DENIED").powerset.map(l => (l.mkString(","), l.map(PhotoStatusTypeProto.valueOf).toVector))
        tests foreach { case (k, exp) => assertEquals(exp, extract("repeated.enum", k, _.ve)) }
    }

    @Test def testRepeatedBoolEmptyString() {
        assertEquals(Vector.empty, extract("repeated.boolean", "", _.vb))
    }

    @Test(expected = classOf[IllegalArgumentException])
    def testRepeatedBoolBadValue() {
        extract("repeated.boolean", "NOT_TRUE", _.vb)
    }

    @Test def testRepeatedBoolTrueUc() {
        val tests = (0 to 5).map(i => (Seq.fill(i)(true), Seq.fill(i)("True").mkString(",")))
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.boolean", s, _.vb)) }
    }

    @Test def testRepeatedBoolTrueLc() {
        val tests = (0 to 5).map(i => (Seq.fill(i)(true), Seq.fill(i)("true").mkString(",")))
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.boolean", s, _.vb)) }
    }

    @Test def testRepeatedBoolTrueAllCaps() {
        val tests = (0 to 5).map(i => (Seq.fill(i)(true), Seq.fill(i)("TRUE").mkString(",")))
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.boolean", s, _.vb)) }
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedIntBadValueFloat() {
        extract("repeated.int", "0f", _.vi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedIntBadValueDouble() {
        extract("repeated.int", "0.0", _.vi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedIntBadValueLong() {
        extract("repeated.int", "1l", _.vi)
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedIntBadValueBig() {
        extract("repeated.int", "2147483648", _.vi)
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedIntValidHexOct() {
        //            val sign = if (math.signum(i) < 0) "-" else ""
        //            val absI = math.abs(i)
        //
        //            val hex = sign + jl.Integer.toHexString(absI)
        //            val _0xHex = sign + "0x" + jl.Integer.toHexString(absI)
        //            val hex = sign + "0x" + jl.Integer.toHexString(absI)
        //            val oct = sign + "0" + jl.Integer.toOctalString(absI)
        //
        //            try {
        //                assertEquals(s"For hexadecimal: $hex", i, csvLines(getLine("required.int" -> hex)).head.i("required.int"))
        //                fail("Hexadecimal should fail")
        //            }
        //            catch {
        //                case e: NumberFormatException =>
        //            }
        //
        //            try {
        //                assertEquals(s"For hexadecimal: $hex", i, csvLines(getLine("required.int" -> hex)).head.i("required.int"))
        //                fail("Hexadecimal should fail")
        //            }
        //            catch {
        //                case e: NumberFormatException =>
        //            }

        fail()
    }

    @Test def testRepeatedIntValid() {
        val tests = (0 to 100).map(i => { val s = Vector.fill(i)(rand.nextInt()); (s, s.mkString(",")) })
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.int", s, _.vi)) }
    }

    @Test def testRepeatedLongEmptyString() {
        assertEquals(Vector.empty, extract("repeated.long", "", _.vl))
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedLongBadValueFloat() {
        assertEquals(Vector.empty, extract("repeated.long", "0f", _.vl))
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedLongBadValueDouble() {
        extract("repeated.long", "0.0", _.vl)
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedLongBadValueBig() {
        extract("repeated.long", "123456789012345678901234567890l", _.vl)
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedLongValidHexOct() {
        fail()
    }

    @Test def testRepeatedLongValid() {
        val tests = (0 to 100).map(i => { val s = Vector.fill(i)(rand.nextLong()); (s, s.mkString(",")) })
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.long", s, _.vl)) }
    }

    @Test def testRepeatedFloatEmptyString() {
        assertEquals(Vector.empty, extract("repeated.float", "", _.vf))
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedFloatBadValueLong() {
        assertEquals(Vector.empty, extract("repeated.float", "1l", _.vf))
    }

    @Test def testRepeatedFloatAsDouble() {
        val asdf: IndexedSeq[Float] = extract("repeated.float", "0.0", _.vf)
        assertEquals(Vector(0f), extract("repeated.float", "0.0", _.vf))
    }

    @Test def testRepeatedFloatBadValueBig() {
        assertEquals(
            Vector(Float.PositiveInfinity),
            extract("repeated.float", "1234567890123456789012345678901234567890f", _.vf)
        )
    }

    @Test def testRepeatedFloatBadValueBigOk() {
        assertEquals(
            Vector(123456789012345678901234567890123456789f),
            extract("repeated.float", "123456789012345678901234567890123456789f", _.vf)
        )
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedFloatValidHexOct() {
        fail()
    }

    @Test
    def testRepeatedFloatValid() {
        val tests = Seq.fill(100)(Seq.fill(rand.nextInt(10))(rand.nextFloat()))
        tests foreach { t => {
            val lstStr = t.mkString(",")
            assertEquals(s"For repeated float : $lstStr", t, extract("repeated.float", lstStr, _.vf))
        }}
    }

    @Test def testRepeatedDoubleEmptyString() {
        assertEquals(Vector.empty, extract("repeated.double", "", _.vd))
    }

    @Test(expected = classOf[NumberFormatException])
    def testRepeatedDoubleBadValueLong() {
        extract("repeated.double", "1l", _.vd)
    }

    @Test def testRepeatedDoubleBadValueDouble() {
        assertEquals(Vector(0f), extract("repeated.double", "0f", _.vd))
    }

    @Test def testRepeatedDoubleBadValueBig() {
        assertEquals(
            Vector(Double.PositiveInfinity),
            extract("repeated.double", "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890d", _.vd)
        )
    }

    @Test def testRepeatedDoubleBadValueBigOk() {
        assertEquals(
            Vector(                     123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d),
            extract("repeated.double", "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d", _.vd)
        )
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedDoubleValidHexOct() {
        fail()
    }

    @Test def testRepeatedDoubleValid() {
        val tests = Seq.fill(100)(Seq.fill(rand.nextInt(10))(rand.nextDouble()))
        tests foreach { t => {
            val lstStr = t.mkString(",")
            assertEquals(s"For repeated double: $lstStr", t, extract("repeated.double", lstStr, _.vd))
        }}
    }

    @Test def testRepeatedEmptyString() {
        assertEquals(s"For string: ''", Vector.empty, extract("repeated.string", "", _.vs))
    }

    @Test def testRepeatedStrings() {
        val tests = Vector.fill(1000)(BigInt(rand.nextString(100).getBytes).toString(32)).grouped(5).toSeq
        tests foreach { s => assertEquals(s"For repeated string: '${s.mkString(",")}'", s, extract("repeated.string", s.mkString(","), _.vs)) }
    }

    // TODO


    @Test def testRepeatedOptEnumEmptyString() {
        assertEquals(Vector.empty, extract("repeated.optional.enum", "", _.voe))
    }

    @Test def testRepeatedOptEnumBadValue() {
        assertEquals(Vector(None), extract("repeated.optional.enum", "NON_EXISTENT_CONSTANT", _.voe))
    }

    @Test def testRepeatedOptEnumBadLeftValue() {
        assertEquals(Vector(None, Some(PhotoStatusTypeProto.ACCEPTED)), extract("repeated.optional.enum", "NON_EXISTENT_CONSTANT,ACCEPTED", _.voe))
    }

    @Test def testRepeatedOptEnumBadRightValue() {
        assertEquals(Vector(Some(PhotoStatusTypeProto.ACCEPTED), None), extract("repeated.optional.enum", "ACCEPTED,NON_EXISTENT_CONSTANT", _.voe))
    }

    @Test def testRepeatedOptEnumBadRightValueSpace() {
        assertEquals(Vector(Some(PhotoStatusTypeProto.ACCEPTED), None), extract("repeated.optional.enum", "ACCEPTED, UPLOADED", _.voe))
    }

    @Test def testRepeatedOptEnumExists() {
        val tests = List("ACCEPTED", "UPLOADED", "DENIED").powerset.map(l => (l.mkString(","), l.map(v => Option(PhotoStatusTypeProto.valueOf(v))).toVector))
        tests foreach { case (k, exp) => assertEquals(exp, extract("repeated.optional.enum", k, _.voe)) }
    }

    @Test def testRepeatedOptBoolEmptyString() {
        assertEquals(Vector.empty, extract("repeated.optional.boolean", "", _.vob))
    }

    @Test def testRepeatedOptBoolBadValue() {
        assertEquals(Vector(None), extract("repeated.optional.boolean", "NOT_TRUE", _.vob))
    }

    @Test def testRepeatedOptBoolTrueUc() {
        val tests = (0 to 5).map(i => (Seq.fill(i)(Option(true)), Seq.fill(i)("True").mkString(",")))
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.optional.boolean", s, _.vob)) }
    }

    @Test def testRepeatedOptBoolTrueLc() {
        val tests = (0 to 5).map(i => (Seq.fill(i)(Option(true)), Seq.fill(i)("true").mkString(",")))
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.optional.boolean", s, _.vob)) }
    }

    @Test def testRepeatedOptBoolTrueAllCaps() {
        val tests = (0 to 5).map(i => (Seq.fill(i)(Option(true)), Seq.fill(i)("TRUE").mkString(",")))
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.optional.boolean", s, _.vob)) }
    }

    @Test def testRepeatedOptIntBadValueFloat() {
        assertEquals(Vector(None), extract("repeated.optional.int", "0f", _.voi))
    }

    @Test def testRepeatedOptIntBadValueDouble() {
        assertEquals(Vector(None), extract("repeated.optional.int", "0.0", _.voi))
    }

    @Test def testRepeatedOptIntBadValueLong() {
        assertEquals(Vector(None), extract("repeated.optional.int", "1l", _.voi))
    }

    @Test def testRepeatedOptIntBadValueBig() {
        assertEquals(Vector(None), extract("repeated.optional.int", "2147483648", _.voi))
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedOptIntValidHexOct() {
        //            val sign = if (math.signum(i) < 0) "-" else ""
        //            val absI = math.abs(i)
        //
        //            val hex = sign + jl.Integer.toHexString(absI)
        //            val _0xHex = sign + "0x" + jl.Integer.toHexString(absI)
        //            val hex = sign + "0x" + jl.Integer.toHexString(absI)
        //            val oct = sign + "0" + jl.Integer.toOctalString(absI)
        //
        //            try {
        //                assertEquals(s"For hexadecimal: $hex", i, csvLines(getLine("required.int" -> hex)).head.i("required.int"))
        //                fail("Hexadecimal should fail")
        //            }
        //            catch {
        //                case e: NumberFormatException =>
        //            }
        //
        //            try {
        //                assertEquals(s"For hexadecimal: $hex", i, csvLines(getLine("required.int" -> hex)).head.i("required.int"))
        //                fail("Hexadecimal should fail")
        //            }
        //            catch {
        //                case e: NumberFormatException =>
        //            }

        fail()
    }

    @Test def testRepeatedOptIntValid() {
        val tests = (0 to 100).map(i => { val s = Vector.fill(i)(rand.nextInt()); (s.map(Option(_)), s.mkString(",")) })
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.optional.int", s, _.voi)) }
    }

    @Test def testRepeatedOptLongEmptyString() {
        assertEquals(Vector.empty, extract("repeated.optional.long", "", _.vol))
    }

    @Test def testRepeatedOptLongBadValueFloat() {
        assertEquals(Vector(None), extract("repeated.optional.long", "0f", _.vol))
    }

    @Test def testRepeatedOptLongBadValueDouble() {
        assertEquals(Vector(None), extract("repeated.optional.long", "0.0", _.vol))
    }

    @Test def testRepeatedOptLongBadValueBig() {
        assertEquals(Vector(None), extract("repeated.optional.long", "123456789012345678901234567890l", _.vol))
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedOptLongValidHexOct() {
        fail()
    }

    @Test def testRepeatedOptLongValid() {
        val tests = (0 to 100).map(i => { val s = Vector.fill(i)(rand.nextLong()); (s.map(Option(_)), s.mkString(",")) })
        tests.foreach { case(exp, s) => assertEquals(exp, extract("repeated.optional.long", s, _.vol)) }
    }

    @Test def testRepeatedOptFloatEmptyString() {
        assertEquals(Vector.empty, extract("repeated.optional.float", "", _.vof))
    }

    @Test def testRepeatedOptFloatBadValueLong() {
        assertEquals(Vector(None), extract("repeated.optional.float", "1l", _.vof))
    }

    @Test def testRepeatedOptFloatAsDouble() {
        assertEquals(Vector(Option(0f)), extract("repeated.optional.float", "0.0", _.vof))
    }

    @Test def testRepeatedOptFloatBadValueBig() {
        assertEquals(
            Vector(Some(Float.PositiveInfinity)),
            extract("repeated.optional.float", "1234567890123456789012345678901234567890f", _.vof)
        )
    }

    @Test def testRepeatedOptFloatBadValueBigOk() {
        assertEquals(
            Vector(Some(123456789012345678901234567890123456789f)),
            extract("repeated.optional.float", "123456789012345678901234567890123456789f", _.vof)
        )
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedOptFloatValidHexOct() {
        fail()
    }

    @Test
    def testRepeatedOptFloatValid() {
        val tests = Seq.fill(100)(Seq.fill(rand.nextInt(10))(Option(rand.nextFloat())))
        tests foreach { t => {
            val lstStr = t.map(_.get).mkString(",")
            assertEquals(s"For repeated float : $lstStr", t, extract("repeated.optional.float", lstStr, _.vof))
        }}
    }

    @Test def testRepeatedOptDoubleEmptyString() {
        assertEquals(Vector.empty, extract("repeated.optional.double", "", _.vod))
    }

    @Test def testRepeatedOptDoubleBadValueLong() {
        assertEquals(Vector(None), extract("repeated.optional.double", "1l", _.vod))
    }

    @Test def testRepeatedOptDoubleBadValueDouble() {
        assertEquals(Vector(Some(0f)), extract("repeated.optional.double", "0f", _.vod))
    }

    @Test def testRepeatedOptDoubleBadValueBig() {
        assertEquals(
            Vector(Some(Double.PositiveInfinity)),
            extract("repeated.optional.double", "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890d", _.vod)
        )
    }

    @Test def testRepeatedOptDoubleBadValueBigOk() {
        assertEquals(
            Vector(Some(                         123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d)),
            extract("repeated.optional.double", "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789d", _.vod)
        )
    }

    @Ignore @Test(expected = classOf[AssertionError])
    def testRepeatedOptDoubleValidHexOct() {
        fail()
    }

    @Test def testRepeatedOptDoubleValid() {
        val tests = Seq.fill(100)(Seq.fill(rand.nextInt(10))(Option(rand.nextDouble())))
        tests foreach { t => {
            val lstStr = t.map(_.get).mkString(",")
            assertEquals(s"For repeated double: $lstStr", t, extract("repeated.optional.double", lstStr, _.vod))
        }}
    }

    @Test def testRepeatedOptEmptyString() {
        assertEquals(s"For string: ''", Vector.empty, extract("repeated.optional.string", "", _.vos))
    }

    @Test def testRepeatedOptStrings() {
        val tests = Vector.fill(1000)(BigInt(rand.nextString(100).getBytes).toString(32)).grouped(5).toSeq.map(_.map(Option(_)))
        tests foreach { s => assertEquals(s"For repeated string: '${s.mkString(",")}'", s, extract("repeated.optional.string", s.map(_.get).mkString(","), _.vos)) }
    }
}

private[csv] object CsvLineTest {
    val FieldSeparator = "\t"
    val IntraFieldSeparator = ","

    val GenderProto =
        Enum("com.eharmony.matching.common.value.ProfileProtoBuffs.ProfileProto.GenderProto",
            "MALE" -> 1,
            "FEMALE" -> 2
        )

    val PhotoStatusTypeProto =
        Enum("com.eharmony.matching.common.value.ProfileProtoBuffs.ProfileProto.PhotoStatusTypeProto",
            "ACCEPTED" -> 5,
            "UPLOADED" -> 1,
            "DENIED" -> 10
        )

    val types = Seq(
        "required.enum"              -> "e",
        "required.boolean"           -> "b",
        "required.int"               -> "i",
        "required.long"              -> "l",
        "required.float"             -> "f",
        "required.double"            -> "d",
        "required.string"            -> "s",

        "optional.enum"              -> "oe",
        "optional.boolean"           -> "ob",
        "optional.int"               -> "oi",
        "optional.long"              -> "ol",
        "optional.float"             -> "of",
        "optional.double"            -> "od",
        "optional.string"            -> "os",

        "repeated.enum"              -> "ve",
        "repeated.boolean"           -> "vb",
        "repeated.int"               -> "vi",
        "repeated.long"              -> "vl",
        "repeated.float"             -> "vf",
        "repeated.double"            -> "vd",
        "repeated.string"            -> "vs",

        "repeated.optional.enum"     -> "voe",
        "repeated.optional.boolean"  -> "vob",
        "repeated.optional.int"      -> "voi",
        "repeated.optional.long"     -> "vol",
        "repeated.optional.float"    -> "vof",
        "repeated.optional.double"   -> "vod",
        "repeated.optional.string"   -> "vos"
    )

    val enums = Map(
        "required.enum"           -> GenderProto,
        "optional.enum"           -> GenderProto,
        "repeated.enum"           -> PhotoStatusTypeProto,
        "repeated.optional.enum"  -> PhotoStatusTypeProto
    )

    val indices = types.unzip._1.zipWithIndex.toMap

    val csvLines = CsvLines(indices, enums)
    val csvLinesMean = CsvLines(indices = indices, enums = enums, errorOnOptMissingEnum = true, errorOnOptMissingField = true)

    def featuresToLineStr(features: (String, String)*): String = {
        val specified = features.flatMap{ case(k, v) => indices.get(k).map(i => (i, v))}
        val unspecified = (indices.values.toSet -- specified.unzip._1).map{(_, "")}.toSeq
        val s = (unspecified ++ specified).sortWith{ _._1 < _._1 }.unzip._2.mkString(FieldSeparator)
        s
    }

    def getCsvLine(csvLines: CsvLines, features: (String, String)*): CsvLine = csvLines(featuresToLineStr(features:_*))

    def extract[A](k: String, v: String, extractor: CsvLine => String => A): A = extract[A](k, v, extractor, csvLines)
    def extractMean[A](k: String, v: String, extractor: CsvLine => String => A): A = extract[A](k, v, extractor, csvLinesMean)


    /** Create a CSV line with a designated key-value pair and then extract the value using the extractor.
      * @param k a column label
      * @param v a value for a given column
      * @param extractor a curried function that given a CsvLine will produce a function from string to some return value.
      * @param csvLines a CSV line creator.
      * @tparam A the return type
      * @return
      */
    private[this] def extract[A](k: String, v: String, extractor: CsvLine => String => A, csvLines: CsvLines): A = {
        val l = try { csvLines(featuresToLineStr(k -> v)) }
        catch {
            case e: Throwable =>
                fail("Shouldn't throw exception.")
                throw new Throwable
        }

        extractor(l)(k)
    }
}
