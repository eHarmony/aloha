package com.eharmony.aloha.models

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.id.ModelId
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ConstantModelTest extends ModelSerializationTestHelper {

  @Test def testSerialization(): Unit = {
    // Right[(Seq[String], Iterable[String]), B]
    val m = ConstantModel(Right(1), ModelId(2, "abc"))
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)

    val m2 = ConstantModel(Left((Seq("1"), Seq("2"))), ModelId(3, "abc"))
    val m3 = serializeDeserializeRoundTrip(m2)
    assertEquals(m2, m3)
  }
}
