package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.score.audit.Semantics
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/15/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class FactoryTest {
  @Test def test1(): Unit = {
    val factory = Factory()
    val sem = Semantics[Any]
    val constant = 1
    val aud = OptionAuditor[Int]
    val model = factory.createConstantModel(sem, OptionTC, aud, constant)
    assertTrue(model.fold(identity, _ => ""), model.isRight)
  }

//  @Test def test2(): Unit = {
//    val constModel: Model[Any, Aux[Option]#TC[Float]] = ConstantModel(ModelId(), 5f, OptionTC, OptionAuditor[Float])
//    HierarchicalConstantModel(ModelId(), OptionTC, 5, OptionAuditor[Int], constModel)
//  }
}
