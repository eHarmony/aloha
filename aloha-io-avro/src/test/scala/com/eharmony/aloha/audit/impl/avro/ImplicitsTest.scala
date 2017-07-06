package com.eharmony.aloha.audit.impl.avro

import com.google.common.collect.Lists
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import scala.collection.JavaConverters.seqAsJavaListConverter
import com.eharmony.aloha.audit.impl.avro.Implicits.{RichFlatScoreList, RichScore}

/**
  * Created by ryan.deak on 7/5/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ImplicitsTest {
  import ImplicitsTest.score
  import FlatScoreListTest.flatScoreList

  @Test def testFlatScoreListToScore(): Unit =
    assertEquals(score, flatScoreList.toScore)

  @Test def testScoreToFlatScoreList(): Unit =
    assertEquals(flatScoreList, score.toFlatScoreList)

  @Test def testRoundTripConversionFromScore(): Unit =
    assertEquals(score, score.toFlatScoreList.toScore)

  @Test def testRoundTripConversionFromFlatScoreList(): Unit =
    assertEquals(flatScoreList, flatScoreList.toScore.toFlatScoreList)
}


object ImplicitsTest {
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