package com.eharmony.aloha.models

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.id.ModelId
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ConstantModelTest extends ModelSerializationTestHelper {

  @Test def testSerialization(): Unit = {
    val m = ConstantModel(Option(1), ModelId(2, "abc"), OptionAuditor[Int]())
    val m1 = serializeDeserializeRoundTrip(m)
    assertEquals(m, m1)

    val m2 = ConstantModel(None: Option[String], ModelId(3, "abc"), OptionAuditor[String]())
    val m3 = serializeDeserializeRoundTrip(m2)
    assertEquals(m2, m3)
  }
}
