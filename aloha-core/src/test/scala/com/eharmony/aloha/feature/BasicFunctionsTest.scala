package com.eharmony.aloha.feature

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import BasicFunctions._

object BasicFunctionsTest {
  type KVPair = Iterable[(String, Double)]
  val one: KVPair = Iterable(("", 1.0))
}

@RunWith(classOf[BlockJUnit4ClassRunner])
class BasicFunctionsTest {
  import BasicFunctionsTest._

  @Test def testByteToSeq(): Unit   = testAnyValToSeq(1.toByte, one)
  @Test def testShortToSeq(): Unit  = testAnyValToSeq(1.toShort, one)
  @Test def testIntToSeq(): Unit    = testAnyValToSeq(1, one)
  @Test def testLongToSeq(): Unit   = testAnyValToSeq(1L, one)
  @Test def testFloatToSeq(): Unit  = testAnyValToSeq(1f, one)
  @Test def testDoubleToSeq(): Unit = testAnyValToSeq(1d, one)

  @Test def testOptByteToSeq(): Unit   = assertEquals(one, Option(1.toByte).toKv)
  @Test def testOptShortToSeq(): Unit  = assertEquals(one, Option(1.toShort).toKv)
  @Test def testOptIntToSeq(): Unit    = assertEquals(one, Option(1).toKv)
  @Test def testOptLongToSeq(): Unit   = assertEquals(one, Option(1L).toKv)
  @Test def testOptFloatToSeq(): Unit  = assertEquals(one, Option(1f).toKv)
  @Test def testOptDoubleToSeq(): Unit = assertEquals(one, Option(1d).toKv)

  @Test def testNoneByteToSeq(): Unit   = testNoneToSeq[Byte]
  @Test def testNoneShortToSeq(): Unit  = testNoneToSeq[Short]
  @Test def testNoneIntToSeq(): Unit    = testNoneToSeq[Int]
  @Test def testNoneLongToSeq(): Unit   = testNoneToSeq[Long]
  @Test def testNoneFloatToSeq(): Unit  = testNoneToSeq[Float]
  @Test def testNoneDoubleToSeq(): Unit = testNoneToSeq[Double]

  def testAnyValToSeq[A](a: A, kv: KVPair)(implicit f: A => KVPair): Unit = assertEquals(kv, f(a))

  def testNoneToSeq[A](implicit f: A => Double): Unit = assertEquals(Nil, Option.empty[A].toKv)
}
