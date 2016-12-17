package com.eharmony.aloha.score.audit.take2

import com.eharmony.aloha.id.ModelId
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

  @Test def test2(): Unit = {
    val constModel = ConstantModel(ModelId(), 5f, OptionTC, OptionAuditor[Float])
    
    // A main design goal is to have type inference work properly so that NO TYPES
    // NEED to be specified to the models.  These are all determined by the inputs.
    // In order for type inference to work properly with no help, we need two
    // parameter lists. Because of this fact, we make a `create` method that has
    // two parameter lists.  This just instantiates the class and fills in the type
    // parameters.  We could have two parameter lists in the case class but then the
    // parameters in the second list aren't part of the Product which is bad.  Also,
    // we can't name the factory method `apply` because when the byte code is generated,
    // it would complain about duplicate method names.
    //
    // "tightest" type: HierarchicalConstantModel[Aux[Option], Any, Float, Int]
    val m = HierarchicalConstantModel(ModelId(), 7, OptionTC, OptionAuditor[Int])(constModel)

    assertEquals(5, m.productArity) // We want all 5 fields to be picked up.

    val y = m(())
    assertEquals(Some(7), y)
  }
}
