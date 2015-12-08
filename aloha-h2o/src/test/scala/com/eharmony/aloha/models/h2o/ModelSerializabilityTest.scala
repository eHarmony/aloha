package com.eharmony.aloha.models.h2o

import com.eharmony.aloha.ModelSerializabilityTestBase
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ModelSerializabilityTest extends ModelSerializabilityTestBase(
  Seq(ModelSerializabilityTest.pkg),
  Seq(
    ".*Test.*",
    ".*\\$.*"
  )
)

object ModelSerializabilityTest {
  def pkg = getClass.getPackage.getName
}