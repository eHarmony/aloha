package com.eharmony.aloha.feature

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import scala.util.Random

@RunWith(classOf[BlockJUnit4ClassRunner])
class BagOfWordsTest {

  val s1 = "the brown fox jumped over the red fence"
  val s2 = "Insurgents killed in ongoing fighting"
  val s3 = "5 of us walked the 8 street with 8 dwarfs"

  @Test def testGrams() {
    val m1 = BagOfWords.bag(s1)
    assertEquals(List(("=the", 1.0), ("=brown", 1.0), ("=fox", 1.0), ("=jumped", 1.0), ("=over", 1.0), ("=the", 1.0), ("=red", 1.0), ("=fence", 1.0)), m1)

    val m2 = BagOfWords.nGrams(s1, 3)
    assertEquals(List(("=the_brown_fox", 1.0), ("=brown_fox_jumped", 1.0), ("=fox_jumped_over", 1.0), ("=jumped_over_the", 1.0), ("=over_the_red", 1.0), ("=the_red_fence", 1.0)), m2)

    val m3 = BagOfWords.skipGrams(s1, 3, 2)
    assertEquals(List(("=the_brown_jumped", 1.0), ("=brown_fox_jumped", 1.0), ("=brown_jumped_over", 1.0), ("=the_fox_jumped", 1.0), ("=fox_jumped_over", 1.0), ("=brown_fox_over", 1.0), ("=the_fox_over", 1.0), ("=the_jumped_over", 1.0), ("=the_brown_fox", 1.0), ("=the_brown_over", 1.0), ("=brown_jumped_the", 1.0), ("=brown_fox_jumped", 1.0), ("=brown_jumped_over", 1.0), ("=brown_fox_the", 1.0), ("=fox_jumped_over", 1.0), ("=jumped_over_the", 1.0), ("=brown_fox_over", 1.0), ("=fox_over_the", 1.0), ("=fox_jumped_the", 1.0), ("=brown_over_the", 1.0), ("=jumped_the_red", 1.0), ("=fox_jumped_over", 1.0), ("=jumped_over_the", 1.0), ("=fox_the_red", 1.0), ("=jumped_over_red", 1.0), ("=fox_over_the", 1.0), ("=fox_over_red", 1.0), ("=fox_jumped_the", 1.0), ("=over_the_red", 1.0), ("=fox_jumped_red", 1.0), ("=jumped_the_red", 1.0), ("=jumped_the_fence", 1.0), ("=the_red_fence", 1.0), ("=over_the_fence", 1.0), ("=jumped_over_the", 1.0), ("=jumped_over_fence", 1.0), ("=jumped_over_red", 1.0), ("=over_red_fence", 1.0), ("=over_the_red", 1.0), ("=jumped_red_fence", 1.0)), m3)

    val m4 = BagOfWords.nGrams(s2, 2)
    assertEquals(List(("=Insurgents_killed", 1.0), ("=killed_in", 1.0), ("=in_ongoing", 1.0), ("=ongoing_fighting", 1.0)), m4)

    val m5 = BagOfWords.skipGrams(s2, 2, 2)
    assertEquals(List(("=killed_in", 1.0), ("=Insurgents_in", 1.0), ("=Insurgents_ongoing", 1.0), ("=in_ongoing", 1.0), ("=killed_ongoing", 1.0), ("=Insurgents_killed", 1.0), ("=killed_in", 1.0), ("=in_fighting", 1.0), ("=killed_fighting", 1.0), ("=ongoing_fighting", 1.0), ("=in_ongoing", 1.0), ("=killed_ongoing", 1.0)), m5)

    val m6 = BagOfWords.nGrams(s2, 3)
    assertEquals(List(("=Insurgents_killed_in", 1.0), ("=killed_in_ongoing", 1.0), ("=in_ongoing_fighting", 1.0)), m6)

    val m7 = BagOfWords.skipGrams(s2, 3, 2)
    assertEquals(List(("=killed_in_fighting", 1.0), ("=Insurgents_in_ongoing", 1.0), ("=Insurgents_ongoing_fighting", 1.0), ("=Insurgents_in_fighting", 1.0), ("=killed_in_ongoing", 1.0), ("=in_ongoing_fighting", 1.0), ("=Insurgents_killed_ongoing", 1.0), ("=killed_ongoing_fighting", 1.0), ("=Insurgents_killed_fighting", 1.0), ("=Insurgents_killed_in", 1.0)), m7)

    val m8 = BagOfWords.bag(s3)
    assertEquals(List(("=5", 1.0), ("=of", 1.0), ("=us", 1.0), ("=walked", 1.0), ("=the", 1.0), ("=8", 1.0), ("=street", 1.0), ("=with", 1.0), ("=8", 1.0), ("=dwarfs", 1.0)), m8)
  }
}
