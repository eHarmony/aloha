package com.eharmony.aloha.audit.impl.avro

import com.google.common.collect.Lists
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import com.eharmony.aloha.audit.impl.avro.AvroScoreAuditorTest.serializeRoundTrip
import scala.collection.JavaConverters.seqAsJavaListConverter
import java.{util => ju}

/**
  * Created by ryan.deak on 7/5/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class FlatScoreListTest {
  import FlatScoreListTest.flatScore

  @Test def testSerializability(): Unit = {
    val serDeserFS =
      serializeRoundTrip(FlatScore.getClassSchema, flatScore).head

    // When comparing the records instead of the JSON strings, equality doesn't
    // hold because they are different types. flatScoreList is a SpecificRecord
    // and SpecificRecord checks if the other values is a SpecificRecord.
    assertEquals(flatScore.toString, serDeserFS.toString)
  }
}

object FlatScoreListTest {
  private[this] def empty[A]: ju.List[A] = ju.Collections.emptyList[A]

  private[this] implicit def toArrayList[A, B](as: Seq[A])(implicit ev: A => B): ju.ArrayList[B] =
    Lists.newArrayList(as.map(ev).asJava)

  private[this] def fsd(value: Any, id: Long, children: Int*): FlatScoreDescendant = {
    new FlatScoreDescendant(
      new ModelId(id, ""),
      value,
      children,
      empty[CharSequence],
      empty[CharSequence],
      null
    )
  }

  private[avro] lazy val flatScore: FlatScore = {
    new FlatScore(new ModelId(1L, ""), 1, Vector(0, 1), empty[CharSequence], empty[CharSequence], null,
      Seq(
        fsd(2L, 2, 2, 3),  // 0
        fsd(3d, 3, 4, 5),  // 1
        fsd(4f, 4),        // 2
        fsd(5,  5),        // 3
        fsd(6d, 6),        // 4
        fsd(7L, 7)         // 5
      )
    )
  }
}
