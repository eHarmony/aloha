package com.eharmony.aloha

import com.eharmony.aloha
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
  * Created by ryan on 12/7/15.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class ModelSerializabilityTest extends ModelSerializabilityTestBase(
  Seq(aloha.pkgName),
  Seq(
    ".*Test.*",
    ".*\\$.*",
    ".*\\.ensemble\\..*"
  )
)
