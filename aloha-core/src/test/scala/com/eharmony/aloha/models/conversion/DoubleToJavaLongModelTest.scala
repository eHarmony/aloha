package com.eharmony.aloha.models.conversion

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.{Model, ErrorModel}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class DoubleToJavaLongModelTest extends ModelSerializationTestHelper {
  @Test def testSerialization(): Unit = {
    val sub = ErrorModel(ModelId(2, "abc"), Seq("def", "ghi"))
    val m = DoubleToJavaLongModel(ModelId(3, "jkl"), sub, 2.3, 3.4, 1, 4, round = true)
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)
  }
}
