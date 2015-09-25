package com.eharmony.aloha.models.h2o

import java.util.Locale

import com.eharmony.aloha.io.StringReadable
import hex.genmodel.GenModel
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
 * Created by deak on 9/21/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CompilerTest {
  @Test def test1(): Unit = {
    val code = StringReadable.fromResource("com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java")
    implicit val locale = Locale.getDefault
    val compiler = new Compiler[GenModel](code)
    val x = compiler.value
    val y: GenModel = x.right.get
    val a = 1
  }
}
