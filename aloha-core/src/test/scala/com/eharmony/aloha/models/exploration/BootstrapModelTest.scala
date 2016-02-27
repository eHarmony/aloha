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
class BootstrapModelTest extends ModelSerializationTestHelper {
  val polices = sci.IndexedSeq(
    ConstantModel(Right(1), ModelId(1, "abc")),
    ConstantModel(Right(2), ModelId(2, "abc")),
    ConstantModel(Right(1), ModelId(3, "abc"))
  )

  @Test def testSerialization(): Unit = {

    val m = BootstrapModel(ModelId(4, "def"), polices, 1, sci.IndexedSeq(1, 2, 3))
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)

    val m2 = BootstrapModel(ModelId(4, "def"), polices, 1, sci.IndexedSeq("1", "2", "3"))
    val m3 = serializeDeserializeRoundTrip(m2)
    assertEquals(m2, m3)
  }
}
