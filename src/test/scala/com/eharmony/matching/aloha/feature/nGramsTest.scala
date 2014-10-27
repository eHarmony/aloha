package com.eharmony.matching.aloha.feature

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import scala.util.Random

@RunWith(classOf[BlockJUnit4ClassRunner])
class nGramTest {

  val s = "the brown fox jumped over the red fence"

  @Test def testBag() {
    val m1 = nGrams.bag(s)
    assertEquals(List(("the", 1.0), ("brown", 1.0), ("fox", 1.0), ("jumped", 1.0), ("over", 1.0), ("the", 1.0), ("red", 1.0), ("fence", 1.0)), m1)

    val m2 = nGrams.nGrams(s, 3)
    assertEquals(List(("the_brown_fox", 1.0), ("brown_fox_jumped", 1.0), ("fox_jumped_over", 1.0), ("jumped_over_the", 1.0), ("over_the_red", 1.0), ("the_red_fence", 1.0)), m2)

    val m3 = nGrams.skipGrams(s, 3, 2)
    assertEquals(List(("brown_jumped_over", 1.0), ("the_fox_over", 1.0), ("fox_jumped_over", 1.0), ("the_brown_fox", 1.0), ("the_brown_over", 1.0), ("the_brown_jumped", 1.0), ("brown_fox_over", 1.0), ("the_fox_jumped", 1.0), ("brown_fox_jumped", 1.0), ("the_jumped_over", 1.0), ("fox_jumped_the", 1.0), ("brown_jumped_over", 1.0), ("fox_over_the", 1.0), ("brown_fox_the", 1.0), ("fox_jumped_over", 1.0), ("jumped_over_the", 1.0), ("brown_over_the", 1.0), ("brown_fox_over", 1.0), ("brown_jumped_the", 1.0), ("brown_fox_jumped", 1.0), ("over_the_red", 1.0), ("fox_jumped_the", 1.0), ("jumped_the_red", 1.0), ("fox_over_the", 1.0), ("fox_jumped_red", 1.0), ("fox_jumped_over", 1.0), ("fox_the_red", 1.0), ("jumped_over_the", 1.0), ("fox_over_red", 1.0), ("jumped_over_red", 1.0), ("over_the_fence", 1.0), ("over_the_red", 1.0), ("jumped_the_red", 1.0), ("over_red_fence", 1.0), ("jumped_over_the", 1.0), ("jumped_red_fence", 1.0), ("jumped_over_fence", 1.0), ("jumped_over_red", 1.0), ("jumped_the_fence", 1.0), ("the_red_fence", 1.0)), m3)
  }
}
