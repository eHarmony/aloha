package com.eharmony.aloha.factory

import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.factory.ex.{AlohaFactoryException, RecursiveModelDefinitionException}
import com.eharmony.aloha.semantics.NoSemantics
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class FactoryImportedModelTest {
  private[this] val factory = ModelFactory.defaultFactory(NoSemantics[Any](), OptionAuditor[Int]())

  @Test(expected = classOf[RecursiveModelDefinitionException])
  def test1CycleDetected() {
    factory.fromResource("com/eharmony/aloha/factory/cycle1_A.json").get
  }

  @Test(expected = classOf[RecursiveModelDefinitionException])
  def test2CycleDetected() {
    factory.fromResource("com/eharmony/aloha/factory/cycle2_A.json").get
  }

  @Test(expected = classOf[RecursiveModelDefinitionException])
  def test3CycleDetected() {
    factory.fromResource("com/eharmony/aloha/factory/cycle3_A.json").get
  }

  @Test def test1LevelSuccessDefault() {
    val m = factory.fromResource("com/eharmony/aloha/factory/success_1_level_default.json").get
    assertEquals(Option(1), m(null))
  }

  @Test def test1LevelSuccessVfs1() {
    val m = factory.fromResource("com/eharmony/aloha/factory/success_1_level_vfs1.json").get
    assertEquals(Option(3), m(null))
  }

  @Test def test1LevelSuccessVfs2() {
    val m = factory.fromResource("com/eharmony/aloha/factory/success_1_level_vfs2.json").get
    assertEquals(Option(4), m(null))
  }

  @Test def test1LevelSuccessFile() {
    val m = factory.fromResource("com/eharmony/aloha/factory/success_1_level_file.json").get
    assertEquals(Option(2), m(null))
  }

  @Test def test1LevelAppropriateFailureWithDefaultProtocol() {
    try {
      factory.fromResource("com/eharmony/aloha/factory/bad_reference_default.json").get
      fail()
    }
    catch {
      case e: AlohaFactoryException => assertTrue(e.getMessage.startsWith("Couldn't resolve VFS2 file"))
      case e: Exception => fail()
    }
  }

  @Test def test1LevelAppropriateFailureWithVfs1() {
    try {
      factory.fromResource("com/eharmony/aloha/factory/bad_reference_vfs1.json").get
      fail()
    }
    catch {
      case e: AlohaFactoryException => assertTrue(e.getMessage, e.getMessage.startsWith("Couldn't resolve VFS1 file"))
      case e: Exception => fail()
    }
  }

  @Test def test1LevelAppropriateFailureWithVfs2() {
    try {
      factory.fromResource("com/eharmony/aloha/factory/bad_reference_vfs2.json").get
      fail()
    }
    catch {
      case e: AlohaFactoryException => assertTrue(e.getMessage.startsWith("Couldn't resolve VFS2 file"))
      case e: Exception => fail()
    }
  }

  @Test def test1LevelApproprivateFailureWithFile() {
    try {
      factory.fromResource("com/eharmony/aloha/factory/bad_reference_file.json").get
      fail()
    }
    catch {
      case e: AlohaFactoryException => assertTrue(e.getMessage.startsWith("Couldn't get JSON for file"))
      case e: Exception => fail()
    }
  }
}
