package com.eharmony.aloha.models.exploration

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.ConstantModel
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits._
import org.junit.Assert._
import org.junit.Test

import scala.collection.{immutable => sci}

/**
  * Created by jmorra on 2/26/16.
  */
class EpsilonGreedyModelTest extends ModelSerializationTestHelper {
  val constantPolicy = ConstantModel(Right(1), ModelId(2, "abc"))

  @Test def testSerialization(): Unit = {
    val m = EpsilonGreedyModel(ModelId(3, "def"), constantPolicy, 0.1f, 1, sci.IndexedSeq(1, 2, 3))
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)

    val m2 = EpsilonGreedyModel(ModelId(3, "def"), constantPolicy, 0.1f, 1, sci.IndexedSeq("1", "2", "3"))
    val m3 = serializeDeserializeRoundTrip(m2)
    assertEquals(m2, m3)
  }
}
