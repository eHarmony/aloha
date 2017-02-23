package com.eharmony.aloha.models.conversion

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.ErrorModel
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import java.{lang => jl}

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class DoubleToJavaLongModelTest extends ModelSerializationTestHelper {
  @Test def testSerialization(): Unit = {
    val sub = ErrorModel(ModelId(2, "abc"), Seq("def", "ghi"), OptionAuditor[Double]())
    val m = DoubleToJavaLongModel(ModelId(3, "jkl"), sub, OptionAuditor[jl.Long](), 2.3, 3.4, 1, 4, round = true)
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)
  }
}
