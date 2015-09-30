package com.eharmony.aloha.models.h2o

import hex.genmodel.GenModel
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
 * Created by deak on 9/21/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CompilerTest {
  @Test def testNoPackage(): Unit = {
    val compiler = new Compiler[GenModel]()
    val genModel = compiler.fromResource("com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java")
    val y: GenModel = genModel.get
    assertTrue(classOf[GenModel].isAssignableFrom(y.getClass))
  }

  @Test def testWithPackage(): Unit = {
    val compiler = new Compiler[GenModel]()
    val genModel = compiler.fromResource("com/eharmony/aloha/models/h2o/domain.glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce37.java")
    val y: GenModel = genModel.get
    assertTrue(classOf[GenModel].isAssignableFrom(y.getClass))
  }
}
