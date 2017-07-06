package com.eharmony.aloha.audit.impl.avro

import com.google.common.collect.Lists
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import scala.collection.JavaConverters.seqAsJavaListConverter
import com.eharmony.aloha.audit.impl.avro.Implicits.{RichFlatScoreList, RichScore}

import java.{lang => jl, util => ju}

/**
  * Created by ryan.deak on 7/5/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ImplicitsTest {
  import ImplicitsTest._
  import FlatScoreListTest.flatScoreList

  @Test def testFlatScoreListToScore(): Unit =
    assertEquals(score, flatScoreList.toScore)

  @Test def testScoreToFlatScoreList(): Unit =
    assertEquals(flatScoreList, score.toFlatScoreList)

  /**
    * Tests that scores are corrected inserted during conversions and that
    * children ordering is preserved.
    */
  @Test def testRoundTripConversionFromScore(): Unit =
    assertEquals(score, score.toFlatScoreList.toScore)

  @Test def testRoundTripConversionFromFlatScoreList(): Unit =
    assertEquals(flatScoreList, flatScoreList.toScore.toFlatScoreList)

  /**
    * Unlike `testRoundTripConversionFromScore`, this tests verifies that
    * all fields in scores are correctly copied during conversions.
    */
  @Test def testAllFieldsAppear(): Unit = {
    val s = new Score(modelId, value, subvalues, errors, missing, prob)
    assertEquals(s, s.toFlatScoreList.toScore)
  }
}


object ImplicitsTest {
  private def modelId = new ModelId(5L, "five")
  private def value: jl.Double = 13d
  private def subvalues = Lists.newArrayList(scr(12L, 8))
  private def errors: ju.List[CharSequence] = Lists.newArrayList("one error", "two errors")
  private def missing: ju.List[CharSequence] =
    Lists.newArrayList("some feature", "another feature", "yet another feature")
  private def prob: jl.Float = 1f

  private lazy val score: Score =
    scr(1, 1,
      scr(2L, 2,
        scr(4f, 4),
        scr(5,  5)
      ),
      scr(3d, 3,
        scr(6d, 6),
        scr(7L, 7)
      )
    )

  private[this] def scr(value: Any, id: Long, children: Score*): Score = {
    new Score(
      new ModelId(id, ""),
      value,
      Lists.newArrayList(children.asJava),
      java.util.Collections.emptyList(),
      java.util.Collections.emptyList(),
      null
    )
  }
}