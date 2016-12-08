package com.eharmony.aloha.feature

import org.junit.Assert._
import org.junit.{Ignore, Test}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import com.eharmony.aloha.feature.MapImplicitRegressionConversion._
import scala.{collection => sc}


@RunWith(classOf[BlockJUnit4ClassRunner])
class SkipGramsTest {

  val insurgent2skip3Grams = wordCount(
    "Insurgents_killed_in",
    "Insurgents_killed_ongoing",
    "Insurgents_killed_fighting",
    "Insurgents_in_ongoing",
    "Insurgents_in_fighting",
    "Insurgents_ongoing_fighting",
    "killed_in_ongoing",
    "killed_in_fighting",
    "killed_ongoing_fighting",
    "in_ongoing_fighting"
  )

  val s1 = "the brown fox jumped over the red fence"
  val s2 = "Insurgents killed in ongoing fighting"
  val s3 = "5 of us walked the 8 street with 8 dwarfs"

  val allTests = Seq(s1, s2, s3)

  @Test def test2SkipTrigrams(): Unit = {
    assertEquals(insurgent2skip3Grams, SkipGrams.skipGrams(s2, 3, 2).coerce)
  }

  @Test def testBagOfWords(): Unit = {
    allTests foreach { s =>
      val bow = SkipGrams.bag(s).coerce
      val exp = kSkipNGramsNaive(s, 1, 0)
      assertEquals(s"For '$s', wrong bag of words: ", exp, bow)
    }
  }

  @Test def testNGrams(): Unit = {
    for (n <- 1 to 4; s <- allTests) {
      val bow = SkipGrams.nGrams(s, n).coerce
      val exp = kSkipNGramsNaive(s, n, 0)
      assertEquals(s"For '$s', wrong $n-grams: ", exp, bow)
    }
  }

  @Ignore @Test def outputSkipGrams(): Unit = {
    val maxK = allTests.map(_.split("""\s+""").length).max - 2
    val data = for {
                 k <- 0 to maxK
                 n <- 1 to 4
                 s <- allTests if n != 1 || k == 0
               } yield (n, k, SkipGrams.skipGrams(s, n, k, "", " ", "").coerce.map(p => (p._1, p._2.toInt)).toVector.sortWith(_._1 < _._1))

    val sorted = data.sortWith { case ((n1, k1, v1), (n2, k2, v2)) =>
      n1 < n2 || (n1 == n2 && v1.head._1 < v2.head._1) || (n1 == n2 && v1.head._1 == v2.head._1 && k1 < k2)
    }

    sorted.foreach { case (n, k, v) => println(s"$n\t$k\t$v") }
  }


  // TODO: Figure out how to test this generically.
  @Ignore @Test def testSkipGrams(): Unit = {
    val maxK = allTests.map(_.split("""\s+""").length).max - 2
    for (k <- 0 to maxK; n <- 1 to 4; s <- allTests if n != 1 || k == 0) {
      val bow = SkipGrams.skipGrams(s, n, k).coerce
      val exp = kSkipNGramsNaive(s, n, k)
      assertEquals(s"For '$s', wrong $k-skip $n-grams: ", exp, bow)
    }
  }

  @Test def testTrigrams(): Unit = {
    allTests foreach { s =>
      val bow = SkipGrams.nGrams(s, 3).coerce
      val exp = kSkipNGramsNaive(s, 3, 0)
      assertEquals(s"For '$s', wrong trigrams: ", exp, bow)
    }
  }


//  @Test def testGrams() {
////    val m1 = SkipGrams.bag(s1).toMap
////    val m1Exp = Map("=the" -> 2.0) ++: Seq("=brown", "=fox", "=jumped", "=over", "=red", "=fence").map(_ -> 1.0).toMap
////    assertEquals(m1Exp, m1)
//
////    val m1_1 = SkipGrams.nGrams(s1.replaceAll("red", "brown"), 2).toMap
////    val m1_1Exp = Map("=the_brown" -> 2.0) ++: Seq("=brown_fox", "=fox_jumped", "=jumped_over", "=over_the", "=brown_fence").map(_ -> 1.0).toMap
////    assertEquals(m1_1Exp, m1_1)
//
//    val m1_2 = SkipGrams.skipGrams(s1, 2, 4)
//    val m1_2Exp = Map("=the_brown" -> 2.0) ++: Seq("=brown_fox", "=fox_jumped", "=jumped_over", "=over_the", "=brown_fence").map(_ -> 1.0).toMap
//    assertEquals(m1_2Exp, m1_2)
//
//
//    val m2 = SkipGrams.nGrams(s1, 3)
//    assertEquals(List(("=the_brown_fox", 1.0), ("=brown_fox_jumped", 1.0), ("=fox_jumped_over", 1.0), ("=jumped_over_the", 1.0), ("=over_the_red", 1.0), ("=the_red_fence", 1.0)), m2)
//
//    val m3 = SkipGrams.skipGrams(s1, 3, 2)
//    assertEquals(List(("=the_brown_jumped", 1.0), ("=brown_fox_jumped", 1.0), ("=brown_jumped_over", 1.0), ("=the_fox_jumped", 1.0), ("=fox_jumped_over", 1.0), ("=brown_fox_over", 1.0), ("=the_fox_over", 1.0), ("=the_jumped_over", 1.0), ("=the_brown_fox", 1.0), ("=the_brown_over", 1.0), ("=brown_jumped_the", 1.0), ("=brown_fox_jumped", 1.0), ("=brown_jumped_over", 1.0), ("=brown_fox_the", 1.0), ("=fox_jumped_over", 1.0), ("=jumped_over_the", 1.0), ("=brown_fox_over", 1.0), ("=fox_over_the", 1.0), ("=fox_jumped_the", 1.0), ("=brown_over_the", 1.0), ("=jumped_the_red", 1.0), ("=fox_jumped_over", 1.0), ("=jumped_over_the", 1.0), ("=fox_the_red", 1.0), ("=jumped_over_red", 1.0), ("=fox_over_the", 1.0), ("=fox_over_red", 1.0), ("=fox_jumped_the", 1.0), ("=over_the_red", 1.0), ("=fox_jumped_red", 1.0), ("=jumped_the_red", 1.0), ("=jumped_the_fence", 1.0), ("=the_red_fence", 1.0), ("=over_the_fence", 1.0), ("=jumped_over_the", 1.0), ("=jumped_over_fence", 1.0), ("=jumped_over_red", 1.0), ("=over_red_fence", 1.0), ("=over_the_red", 1.0), ("=jumped_red_fence", 1.0)), m3)
//
//    val m4 = SkipGrams.nGrams(s2, 2)
//    assertEquals(List(("=Insurgents_killed", 1.0), ("=killed_in", 1.0), ("=in_ongoing", 1.0), ("=ongoing_fighting", 1.0)), m4)
//
//    val m5 = SkipGrams.skipGrams(s2, 2, 2)
//    assertEquals(List(("=killed_in", 1.0), ("=Insurgents_in", 1.0), ("=Insurgents_ongoing", 1.0), ("=in_ongoing", 1.0), ("=killed_ongoing", 1.0), ("=Insurgents_killed", 1.0), ("=killed_in", 1.0), ("=in_fighting", 1.0), ("=killed_fighting", 1.0), ("=ongoing_fighting", 1.0), ("=in_ongoing", 1.0), ("=killed_ongoing", 1.0)), m5)
//
//    val m6 = SkipGrams.nGrams(s2, 3)
//    assertEquals(List(("=Insurgents_killed_in", 1.0), ("=killed_in_ongoing", 1.0), ("=in_ongoing_fighting", 1.0)), m6)
//
//    val m7 = SkipGrams.skipGrams(s2, 3, 2)
//    assertEquals(List(("=killed_in_fighting", 1.0), ("=Insurgents_in_ongoing", 1.0), ("=Insurgents_ongoing_fighting", 1.0), ("=Insurgents_in_fighting", 1.0), ("=killed_in_ongoing", 1.0), ("=in_ongoing_fighting", 1.0), ("=Insurgents_killed_ongoing", 1.0), ("=killed_ongoing_fighting", 1.0), ("=Insurgents_killed_fighting", 1.0), ("=Insurgents_killed_in", 1.0)), m7)
//
//    val m8 = SkipGrams.bag(s3)
//    assertEquals(List(("=5", 1.0), ("=of", 1.0), ("=us", 1.0), ("=walked", 1.0), ("=the", 1.0), ("=8", 1.0), ("=street", 1.0), ("=with", 1.0), ("=8", 1.0), ("=dwarfs", 1.0)), m8)
//  }




  @Test def test4SkipBigramsNaive(): Unit = {
    val expected = wordCount(
      "the_brown", "the_fox", "the_jumped", "the_over", "the_the",
      "brown_fox", "brown_jumped", "brown_over", "brown_the", "brown_red",
      "fox_jumped", "fox_over", "fox_the", "fox_red", "fox_fence",
      "jumped_over", "jumped_the", "jumped_red", "jumped_fence",
      "over_the", "over_red", "over_fence",
      "the_red", "the_fence",
      "red_fence"
    )

    assertEquals(expected, kSkipNGramsNaive(s1, 2, 4))
  }

  @Test def test2SkipTrigramsNaive(): Unit =
    assertEquals(insurgent2skip3Grams, kSkipNGramsNaive(s2, 3, 2))

  /**
   * A word count implementation, used for testing the testing code.
   * @param vals a bunch of tokens
   * @return
   */
  private[this] def wordCount(vals: String*) = vals.map("=" + _).groupBy(identity).mapValues(_.size.toDouble)

  /**
   * Reference implementation of k-skip n-grams for ''n'' in {1, 2, 3, 4}. This is used for testing any more
   * performant implementations.
   * @param s string from which features are extracted
   * @param n size of each subset of tokens (''n'' in {1, 2, 3, 4})
   * @param k maximum token skip distance
   * @return
   */
  private[this] def kSkipNGramsNaive(s: String, n: Int, k: Int): sc.Map[String, Double] = n match {
    case 1 if k == 0 => oneGramsNaive(s)
    case 2           => kSkipBigramsNaive(s, k)
    case 3           => kSkipTrigramsNaive(s, k)
    case 4           => kSkip4GramsNaive(s, k)
  }

  /**
   * Reference implementation of 2-grams
   * @param s string from which features are extracted
   * @return
   */
  private[this] def oneGramsNaive(s: String): sc.Map[String, Double] = {
    val result = s split """\s+""" groupBy identity  map { case (k, v) => "=" + k -> v.length.toDouble }
    result
  }

  /**
   * Reference implementation of k-skip 2-grams
   * @param s string from which features are extracted
   * @param k maximum token skip distance
   * @return
   */
  private[this] def kSkipBigramsNaive(s: String, k: Int): sc.Map[String, Double] = {
    val tokens = s.split("""\s+""")
    val n = tokens.length
    val result = (for {
                    i <- 0 until n
                    j <- i + 1 to math.min(i + k + 1, n - 1)
                  } yield "=" + tokens(i) + "_" + tokens(j)
                 ).groupBy(identity).
                   mapValues(_.size.toDouble)
    result
  }

  /**
   * Reference implementation of k-skip 3-grams
   * @param s string from which features are extracted
   * @param k maximum token skip distance
   * @return
   */
  private[this] def kSkipTrigramsNaive(s: String, k: Int): sc.Map[String, Double] = {
    val tokens = s.split("""\s+""")
    val n = tokens.length
    val result = (for {
                    i <- 0 until n
                    j <- i + 1 to math.min(i + k + 1, n - 1)
                    m <- j + 1 to math.min(j + k + 1, n - 1)
                  } yield "=" + tokens(i) + "_" + tokens(j) + "_" + tokens(m)
                 ).groupBy(identity).
                   mapValues(_.size.toDouble)
    result
  }

  /**
   * Reference implementation of k-skip 3-grams
   * @param s string from which features are extracted
   * @param k maximum token skip distance
   * @return
   */
  private[this] def kSkip4GramsNaive(s: String, k: Int): sc.Map[String, Double] = {
    val tokens = s.split("""\s+""")
    val n = tokens.length
    val result = (for {
      i <- 0 until n
      j <- i + 1 to math.min(i + k + 1, n - 1)
      m <- j + 1 to math.min(j + k + 1, n - 1)
      o <- m + 1 to math.min(m + k + 1, n - 1)
    } yield "=" + tokens(i) + "_" + tokens(j) + "_" + tokens(m) + "_" + tokens(o)
        ).groupBy(identity).
        mapValues(_.size.toDouble)
    result
  }

  private[this] implicit class MapCoercion[C](val m: sc.Map[String, C]) {
    def coerce(implicit ev: sc.Map[String, C] => sc.Map[String, Double]) = ev(m)
  }
}
