package com.eharmony.aloha.models.tree.decision

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.{TopLevelModel, ErrorModel}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class BasicDecisionTreeTest extends ModelSerializationTestHelper {
  @Test def testSerialization(): Unit = {
    val m = BasicDecisionTree(ModelId(2, "abc"), root = Leaf(3), returnBest = true)
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)
  }
}
