package com.eharmony.matching.aloha.feature

import com.eharmony.matching.notaloha.AnEnum
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.util.Random

@RunWith(classOf[BlockJUnit4ClassRunner])
class IndicatorTest {

    private[this] implicit val rand = new Random(0)

    private[this] val numRandTests = 1000

    @Test def testEnum() {
        AnEnum.values foreach { e => assertEquals(s"For enum $e: ", Seq((s"=${e.name}", 1d)), BasicFunctions.ind(e)) }
    }

    @Test def testUnit() {
        val unit: Unit = ()
        assertEquals(s"For unit: ", expected("()"), BasicFunctions.ind(unit))
    }

    @Test def testBoolean() {
        Seq(true, false) foreach { t => assertEquals(s"For boolean $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testByte() {
        val tests = new Array[Byte](numRandTests)
        rand.nextBytes(tests)
        tests foreach { t => assertEquals(s"For byte $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testChar() {
        val tests = Seq.fill(numRandTests)(rand.nextPrintableChar())
        tests foreach { t => assertEquals(s"For char $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testShort() {
        val tests = Seq.fill(numRandTests)((rand.nextInt() % (Short.MaxValue + 1)).toShort)
        tests foreach { t => assertEquals(s"For short $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testInt() {
        val tests = Seq.fill(numRandTests)(rand.nextInt())
        tests foreach { t => assertEquals(s"For int $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testLong() {
        val tests = Seq.fill(numRandTests)(rand.nextLong())
        tests foreach { t => assertEquals(s"For long $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testFloat() {
        val tests = Seq.fill(numRandTests)(rand.nextFloat())
        tests foreach { t => assertEquals(s"For float $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testDouble() {
        val tests = Seq.fill(numRandTests)(rand.nextDouble())
        tests foreach { t => assertEquals(s"For double $t: ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testString() {
        val tests = Seq.fill(numRandTests)(BigInt(rand.nextString(100).getBytes).toString(32))
        tests foreach { t => assertEquals(s"For string '$t': ", expected(t), BasicFunctions.ind(t)) }
    }

    @Test def testSomeEnum() {
        AnEnum.values.map(Some.apply) foreach { e => assertEquals(s"For some enum $e: ", expected(e.get), BasicFunctions.ind(e)) }
    }

    @Test def testSomeUnit() {
        val unit = ()
        assertEquals(s"For unit: ", expected("()"), BasicFunctions.ind(unit))
    }

    @Test def testSomeBoolean() {
        Seq(true, false).map(b => Some(b)) foreach { t => assertEquals(s"For some boolean $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeByte() {
        val bytes = new Array[Byte](numRandTests)
        rand.nextBytes(bytes)
        val tests = bytes.map(b => Some(b))
        tests foreach { t => assertEquals(s"For some byte $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeChar() {
        val tests = Seq.fill(numRandTests)(Some(rand.nextPrintableChar()))
        tests foreach { t => assertEquals(s"For some char $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeShort() {
        val tests = Seq.fill(numRandTests)(Some((rand.nextInt() % (Short.MaxValue + 1)).toShort))
        tests foreach { t => assertEquals(s"For some short $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeInt() {
        val tests = Seq.fill(numRandTests)(Some(rand.nextInt()))
        tests foreach { t => assertEquals(s"For some int $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeLong() {
        val tests = Seq.fill(numRandTests)(Some(rand.nextLong()))
        tests foreach { t => assertEquals(s"For some long $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeFloat() {
        val tests = Seq.fill(numRandTests)(Some(rand.nextFloat()))
        tests foreach { t => assertEquals(s"For some float $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeDouble() {
        val tests = Seq.fill(numRandTests)(Some(rand.nextDouble()))
        tests foreach { t => assertEquals(s"For some double $t: ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeString() {
        val tests = Seq.fill(numRandTests)(Some(BigInt(rand.nextString(100).getBytes).toString(32)))
        tests foreach { t => assertEquals(s"For some string '$t': ", expected(t.get), BasicFunctions.ind(t)) }
    }

    @Test def testSomeStringList() {
      val tests = IndexedSeq.fill(numRandTests)((BigInt(rand.nextString(100).getBytes).toString(32)))
      assertEquals(s"For list of strings: ", expected(tests), BasicFunctions.ind(tests))
    }

    @Test def testNoneEnum(): Unit = testMissingOption[AnEnum]()
    @Test def testNoneUnit(): Unit = testMissingOption[Unit]()
    @Test def testNoneBoolean(): Unit = testMissingOption[Boolean]()
    @Test def testNoneByte(): Unit = testMissingOption[Byte]()
    @Test def testNoneChar(): Unit = testMissingOption[Char]()
    @Test def testNoneShort(): Unit = testMissingOption[Short]()
    @Test def testNoneInt(): Unit = testMissingOption[Int]()
    @Test def testNoneLong(): Unit = testMissingOption[Long]()
    @Test def testNoneFloat(): Unit = testMissingOption[Float]()
    @Test def testNoneDouble(): Unit = testMissingOption[Double]()
    @Test def testNoneString(): Unit = testMissingOption[String]()

    private[this] def testMissingOption[A: Manifest]() {
        val t: Option[A] = None
        val typeStr = implicitly[Manifest[A]].toString()
        assertEquals(s"For missing $typeStr: ", BasicFunctions.DefaultForMissingDataInReg, BasicFunctions.ind(t))
    }

    private[this] def expected[A](a: A): Iterable[(String, Double)] = Seq((s"=$a", 1d))

    private[this] def expected[A](a: IndexedSeq[String]): Iterable[(String, Double)] = a.map(b => ((s"=$b", 1.0)))
}
