package com.eharmony.aloha.models.h2o.compiler

import hex.genmodel.GenModel
import hex.genmodel.easy.prediction.RegressionModelPrediction
import hex.genmodel.easy.{RowData, EasyPredictModelWrapper}
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
    val compiler = new Compiler[GenModel]
    val genModel = compiler.fromResource("com/eharmony/aloha/models/h2o/glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce38.java")
    val y: GenModel = genModel.get
    assertTrue(classOf[GenModel].isAssignableFrom(y.getClass))

    val x = new RowData
    x.put("Sex", "F")
    x.put("Length", java.lang.Double.valueOf(0.0))
    x.put("Diameter", java.lang.Double.valueOf(0.0))
    x.put("Height", java.lang.Double.valueOf(0.0))
    x.put("Whole weight", java.lang.Double.valueOf(0.0))
    x.put("Shucked weight", java.lang.Double.valueOf(0.0))
    x.put("Viscera weight", java.lang.Double.valueOf(0.0))
    x.put("Shell weight", java.lang.Double.valueOf(0.0))
    println(new EasyPredictModelWrapper(y).predictRegression(x).value)
  }

  @Test def testWithPackage(): Unit = {
    val compiler = new Compiler[GenModel]()
    val genModel = compiler.fromResource("com/eharmony/aloha/models/h2o/domain.glm_afa04e31_17ad_4ca6_9bd1_8ab80005ce37.java")
    val y: GenModel = genModel.get
    assertTrue(classOf[GenModel].isAssignableFrom(y.getClass))
  }

  @Test def testDrfCompiles(): Unit = {
    val compiler = new Compiler[GenModel]()
    val modelTry = compiler.fromResource("com/eharmony/aloha/models/h2o/DRF_model_1463074092542_1.java")
    val model = new EasyPredictModelWrapper(modelTry.get)
    val complexPrediction = model.predict(new RowData)
    val pred = complexPrediction match {
      case r: RegressionModelPrediction => Option(r.value)
      case _ => None
    }
    assertEquals(Option(0.0), pred)
  }
}
