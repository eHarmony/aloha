package com.eharmony.aloha.score.audit

import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.score.audit.AuditTest._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/9/16.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class AuditTest {
  @Test def test1(): Unit = {
    val na: MorphableAuditor[Long, Int, Option[Int], NoAudit[Long, Int]] = NoAudit[Long, Int]()
    val impl = na.auditor[Float].get
    val audit = impl.success(1L, 1.5f) // impl.audit(1L, 1.5f)
    assertEquals(Option(1.5f), audit.map(floatIdentity))
  }

  @Test def test2(): Unit = {
    val sem = Sem("test", NoAudit[Long, Double]())
    val auditVal = sem.audit(6.toString).flatten
    assertEquals(Option("6"), auditVal)
  }

  @Test def test3(): Unit = {
    val na = NoAudit[Long, String]()
    val sna = na.auditor[String].flatMap(a => a.success(1L, "6"))
    assertEquals(Option("6"), sna)
  }
}

object AuditTest {
  def floatIdentity(f: Float) = f
  private[audit] case class Sem[In, Aud <: MorphableAuditor[Long, Nothing, Any, Aud]](prototype: In, aud: Aud) {
    def audit[V: RefInfo](v: V): Option[Aud#AuditOutput[V]] = aud.auditor[V].map(a => a.success(1L, v))
  }
}
