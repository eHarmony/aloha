package com.eharmony.aloha.models

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.id.ModelId
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class TopLevelModelTest extends ModelSerializationTestHelper {
  @Test def testSerialization(): Unit = {
    val sub = ErrorModel(ModelId(2, "abc"), Seq("def", "ghi"), OptionAuditor[Int]())
    val m = TopLevelModel(sub, Seq("jkl"))
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)
  }
}
