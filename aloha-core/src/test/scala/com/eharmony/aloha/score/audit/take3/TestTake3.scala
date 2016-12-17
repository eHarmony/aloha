package com.eharmony.aloha.score.audit.take3

import com.eharmony.aloha.id.ModelId
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/16/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class TestTake3 {
  @Test def test1(): Unit = {
    val cm = ConstantModel(ModelId(), 1, OptionAuditor[Int])
    val constant = 2f;
    val hcm = HierarchicalConstantModel(ModelId(), constant, OptionAuditor[Float])(cm)
    val hcmModel: Model[Any, Option[Float]] = hcm

    assertEquals(Option(constant), hcmModel(None))
  }
}
