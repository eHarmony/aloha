package com.eharmony.aloha.models.vw.jni

import java.io.FileInputStream

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.models.reg.ConstantDeltaSpline
import com.eharmony.matching.testhelp.io.IoCaptureCompanion
import org.apache.commons.vfs2.VFS
import org.junit.Assert._
import org.junit.{BeforeClass, Test}
import spray.json.pimpString

/**
 * Created by jmorra on 7/10/15.
 */
object VwJniModelCreatorTest extends IoCaptureCompanion {
  @BeforeClass def createModel(): Unit = VwJniModelTest.createModel()
  def vwModelPath = VwJniModelTest.VwModelPath

  lazy val base64EncodedModelString = VwJniModel.readModel(new FileInputStream(vwModelPath))
  val vfs = VFS.getManager
  val vfsModel = vfs.resolveFile(vwModelPath)
  val vfsSpec = vfs.resolveFile("res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js")
  val cds = ConstantDeltaSpline(0, 1, IndexedSeq(0.25, 0.75))
}

class VwJniModelCreatorTest {
  import VwJniModelCreatorTest._

  @Test def testGoodModel() = {

    val expected =
      ("""
         |{
         |  "modelType": "VwJNI",
         |  "modelId": { "id": 0, "name": "model name" },
         |  "features": {
         |    "height_mm": "Seq((\"1800\", 1.0))"
         |  },
         |  "namespaces": {
         |    "personal_features": [ "height_mm" ]
         |  },
         |  "vw": {
         |    "params": "--quiet -t",
         |    "model": """".stripMargin.trim + base64EncodedModelString + """"
                                                                            |  }
                                                                            |}
                                                                          """).stripMargin.parseJson
    val actual = VwJniModelCreator.buildModel(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"))
    assertEquals(actual, expected)
  }

  @Test def withNotes() = {
    val expected =
      ("""
         |{
         |  "modelType": "VwJNI",
         |  "modelId": { "id": 0, "name": "model name" },
         |  "features": {
         |    "height_mm": "Seq((\"1800\", 1.0))"
         |  },
         |  "notes": [
         |    "This is a note"
         |  ],
         |  "namespaces": {
         |    "personal_features": [ "height_mm" ]
         |  },
         |  "vw": {
         |    "params": "--quiet -t",
         |    "model": """".stripMargin.trim + base64EncodedModelString + """"
                                                                            |  }
                                                                            |}
                                                                          """).stripMargin.parseJson
    val actual = VwJniModelCreator.buildModel(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), None, Some(Seq("This is a note")))
    assertEquals(actual, expected)
  }

  @Test def withSpline() = {
    val expected =
      ("""
         |{
         |  "modelType": "VwJNI",
         |  "modelId": { "id": 0, "name": "model name" },
         |  "features": {
         |    "height_mm": "Seq((\"1800\", 1.0))"
         |  },
         |  "spline": {
         |    "min": 0.0,
         |    "max": 1.0,
         |    "knots": [0.25, 0.75]
         |  },
         |  "namespaces": {
         |    "personal_features": [ "height_mm" ]
         |  },
         |  "vw": {
         |    "params": "--quiet -t",
         |    "model": """".stripMargin.trim + base64EncodedModelString + """"
                                                                            |  }
                                                                            |}
                                                                          """).stripMargin.parseJson

    val actual = VwJniModelCreator.buildModel(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), None, None, Some(cds))
    assertEquals(actual, expected)
  }

  @Test def withNotesAndSpline() = {
    val expected =
      ("""
         |{
         |  "modelType": "VwJNI",
         |  "modelId": { "id": 0, "name": "model name" },
         |  "features": {
         |    "height_mm": "Seq((\"1800\", 1.0))"
         |  },
         |  "notes": [
         |    "This is a note"
         |  ],
         |  "spline": {
         |    "min": 0.0,
         |    "max": 1.0,
         |    "knots": [0.25, 0.75]
         |  },
         |  "namespaces": {
         |    "personal_features": [ "height_mm" ]
         |  },
         |  "vw": {
         |    "params": "--quiet -t",
         |    "model": """".stripMargin.trim + base64EncodedModelString + """"
                                                                            |  }
                                                                            |}
                                                                          """).stripMargin.parseJson
    val actual = VwJniModelCreator.buildModel(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), None, Some(Seq("This is a note")), Some(cds))
    assertEquals(actual, expected)
  }
}
