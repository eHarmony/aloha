package com.eharmony.aloha.score.audit.take5.scoreproto

import com.eharmony.aloha.id.ModelId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 1/17/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ScoreAuditorTest {
  @Test def testBooleanUnapply(): Unit = {
    val a = ScoreAuditor.booleanAuditor
//    val s = a.unapply(ScoreAuditor.byteAuditor.success(ModelId(1, "one"), 1.toByte, Set.empty, Nil, None))
//    assertEquals(None, s)
  }
}
