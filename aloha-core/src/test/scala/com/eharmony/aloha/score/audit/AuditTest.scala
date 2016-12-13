package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.{ModelId, ModelIdentity}
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/9/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class AuditTest {
  @Test def verifyRetrievalOfAuditorWithDifferentType(): Unit = {
    val intOptAud: MorphableAuditor[Long, Int, Option[Int], OptionAuditor[Long, Int]] = OptionAuditor[Long, Int]
    val floatOptAud = intOptAud.auditor[Float].get
    val audit = floatOptAud.success(1L, 1.5f)
    assertEquals(Option(1.5f), audit.map(floatIdentity))
  }

  @Test def verifyFactoryCreationAndBasicModelCreation(): Unit = {
    val x = 1d
    val oa = OptionAuditor[ModelIdentity, Double]
    val f = StdModelFactory()
    val s = Semantics[Any]()
    val model = f.createConstantModel(s, oa, ModelId(1, "test"), x).right.get
    val y = model(())
    assertEquals(Option(x), y)
  }

  @Test def test3(): Unit = {
    val na = OptionAuditor[Long, String]
    val sna = na.auditor[String].flatMap(a => a.success(1L, "6"))
    assertEquals(Option("6"), sna)
  }

  @Test def test4(): Unit = {
    val cI = 1d
    val idI = ModelId(1, "one")
    val aO = OptionAuditor[ModelIdentity, Float]
    val sub = ConstantModel(idI, cI, aO.auditor[Double].get)

    val cO = 2f
    val idO = ModelId(2, "two")
    val model = HierarchicalConstantModel(idO, cO, aO)(sub)
    val y = model(())
    assertEquals(Option(cO), y)
  }

  private[this] def floatIdentity(f: Float) = f
}
