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
 * These tests are now designed to pass if the VW model cannot be created in the BeforeClass method.
 * This is due to Travis not working as we expect it to.  Because cat /proc/version doesn't match
 * the purported os the VW JNI library doesn't know which system dependent version of the lib
 * to load and these tests will consequently fail.
 *
 * Created by jmorra on 7/10/15.
 */
object VwJniModelJsonTest extends IoCaptureCompanion {
  @BeforeClass def createModel(): Unit = VwJniModelTest.createModel()
  lazy val base64EncodedModelString = Option(VwJniModelTest.VwModelFile.exists).collect{case true => VwJniModel.readBinaryVwModelToB64String(new FileInputStream(VwJniModelTest.VwModelFile))}
  val vfs = VFS.getManager
  val vfsModel = vfs.resolveFile(VwJniModelTest.VwModelPath)
  val vfsSpec = vfs.resolveFile("res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js")
  val cds = ConstantDeltaSpline(0, 1, IndexedSeq(0.25, 0.75))
}

class VwJniModelJsonTest {
  import VwJniModelJsonTest._

  @Test def testGoodModel() = {
    base64EncodedModelString foreach { m =>
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
               |    "model": """".stripMargin.trim + m + """"
               |  }
               |}
             """).stripMargin.parseJson
        val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"))
        assertEquals(expected, actual)
    }
  }

  @Test def withNotes() = {
      base64EncodedModelString foreach { m =>
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
                 |    "model": """".stripMargin.trim + m + """"
                 |  }
                 |}
               """).stripMargin.parseJson
          val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), None, Some(Seq("This is a note")))
          assertEquals(expected, actual)
      }
  }

  @Test def withSpline() = {
      base64EncodedModelString foreach { m =>
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
                 |    "model": """".stripMargin.trim + m + """"
                 |  }
                 |}
               """).stripMargin.parseJson

          val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), None, None, Some(cds))
          assertEquals(expected, actual)
      }
  }

  @Test def withNotesAndSpline() = {
      base64EncodedModelString foreach { m =>
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
                 |    "model": """".stripMargin.trim + m + """"
                 |  }
                 |}
               """).stripMargin.parseJson
          val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), None, Some(Seq("This is a note")), Some(cds))
          assertEquals(expected, actual)
      }
  }
}
