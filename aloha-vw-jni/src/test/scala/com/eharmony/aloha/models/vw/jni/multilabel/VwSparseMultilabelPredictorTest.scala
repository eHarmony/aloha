package com.eharmony.aloha.models.vw.jni.multilabel

import java.io.{ByteArrayOutputStream, File, FileInputStream}

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.io.sources.{Base64StringSource, ExternalSource, ModelSource}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import vowpalWabbit.learner.{VWActionScoresLearner, VWLearners}

/**
  * Created by ryan.deak on 9/27/17.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwSparseMultilabelPredictorTest extends ModelSerializationTestHelper {
  import VwSparseMultilabelPredictorTest._

  @Test def testSerializability(): Unit = {
    val predictor = getPredictor(getModelSource(), 3)
    val ds = serializeDeserializeRoundTrip(predictor)
    assertEquals(predictor, ds)
    assertEquals(predictor.vwParams(), ds.vwParams())
    assertNotNull(ds.vwModel)
  }

  @Test def testVwParameters(): Unit = {
    val numLabelsInTrainingSet = 3
    val predictor = getPredictor(getModelSource(), numLabelsInTrainingSet)

    predictor.vwParams() match {
      case Data(vwBinFilePath, ringSize) =>
        checkVwBinFile(vwBinFilePath)
        checkVwRingSize(numLabelsInTrainingSet, ringSize.toInt)
      case ps => fail(s"Unexpected VW parameters format.  Found string: $ps")
    }
  }
}

object VwSparseMultilabelPredictorTest {
  private val Data = """\s*-i\s+(\S+)\s+--ring_size\s+(\d+)\s+--testonly\s+--quiet""".r

  private def getModelSource(): ModelSource = {
    val f = File.createTempFile("i_dont", "care")
    f.deleteOnExit()
    val learner = VWLearners.create[VWActionScoresLearner](s"--quiet --csoaa_ldf mc --csoaa_rank -f ${f.getCanonicalPath}")
    learner.close()
    val baos = new ByteArrayOutputStream()
    IOUtils.copy(new FileInputStream(f), baos)
    val src = Base64StringSource(Base64.encodeBase64URLSafeString(baos.toByteArray))
    ExternalSource(src.localVfs)
  }

  private def getPredictor(modelSrc: ModelSource, numLabelsInTrainingSet: Int) =
    VwSparseMultilabelPredictor[Any](modelSrc, Nil, Nil, numLabelsInTrainingSet)

  private def checkVwBinFile(vwBinFilePath: String): Unit = {
    val vwBinFile = new File(vwBinFilePath)
    assertTrue("VW binary file should have been written to disk", vwBinFile.exists())
    vwBinFile.deleteOnExit()
  }

  private def checkVwRingSize(numLabelsInTrainingSet: Int, ringSize: Int): Unit = {
    assertEquals(
      "vw --ring_size parameter is incorrect:",
      numLabelsInTrainingSet + VwSparseMultilabelPredictor.AddlVwRingSize,
      ringSize.toInt
    )
  }
}