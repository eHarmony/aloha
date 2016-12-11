package com.eharmony.aloha.score.audit

import com.eharmony.aloha.id.ModelId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/9/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class AuditTest {
  @Test def test1(): Unit = {
    val na: MorphableAuditor[Long, Int, Option[Int], NoAudit[Long, Int]] = NoAudit[Long, Int]
    val impl = na.auditor[Float].get
    val audit = impl.success(1L, 1.5f)
    assertEquals(Option(1.5f), audit.map(floatIdentity))
  }

  @Test def test2(): Unit = {
    val na = NoAudit[ModelId, Double]
    val f = ModelFactories.create(na)
    val x = 1d
    val model = f.createConstantModel(Semantics[Any](), ModelId(1, "test"), x).right.get
    val y = model(x)
    assertEquals(Option(x), y)
  }

  @Test def test3(): Unit = {
    val na = NoAudit[Long, String]
    val sna = na.auditor[String].flatMap(a => a.success(1L, "6"))
    assertEquals(Option("6"), sna)
  }

  private[this] def floatIdentity(f: Float) = f
}
