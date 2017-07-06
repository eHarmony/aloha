package com.eharmony.aloha.audit.impl.avro

import com.google.common.collect.Lists
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.JavaConverters.seqAsJavaListConverter

/**
  * Created by ryan.deak on 7/5/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class FlatScoreListTest {
  import FlatScoreListTest.flatScoreList

  @Test def testSerializability(): Unit = {
    val serDeserFlatScoreList =
      AvroScoreAuditorTest.serializeRoundTrip(FlatScoreList.SCHEMA$, flatScoreList).head

    // When comparing the records instead of the JSON strings, equality doesn't
    // hold because they are different types. flatScoreList is a SpecificRecord
    // and SpecificRecord checks if the other values is a SpecificRecord.
    assertEquals(flatScoreList.toString, serDeserFlatScoreList.toString)
  }
}

object FlatScoreListTest {
  private[this] def flatScore(value: Any, id: Long, children: Int*): FlatScore = {
    new FlatScore(
      new ModelId(id, ""),
      value,
      Lists.newArrayList(children.map(i => java.lang.Integer.valueOf(i)).asJava),
      java.util.Collections.emptyList(),
      java.util.Collections.emptyList(),
      null
    )
  }

  private[avro] lazy val flatScoreList: FlatScoreList =
    new FlatScoreList(
      Lists.newArrayList(
        flatScore(1,  1, 1, 2),  // 0
        flatScore(2L, 2, 3, 4),  // 1
        flatScore(3d, 3, 5, 6),  // 2
        flatScore(4f, 4),        // 3
        flatScore(5,  5),        // 4
        flatScore(6d, 6),        // 5
        flatScore(7L, 7)         // 6
      )
    )
}
