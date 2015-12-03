package com.eharmony.aloha.models.vw.jni

import java.io.FileInputStream

import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.io.vfs.{File, Vfs1, Vfs2}
import com.eharmony.aloha.models.reg.ConstantDeltaSpline
import com.eharmony.matching.testhelp.io.IoCaptureCompanion
import org.apache.commons.{vfs => vfs1, vfs2}
import org.junit.Assert._
import org.junit.{BeforeClass, Test}
import spray.json.{JsObject, pimpString}


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
  lazy val base64EncodedModelString = VwJniModel.readBinaryVwModelToB64String(new FileInputStream(VwJniModelTest.VwModelFile))
  val vfs = vfs2.VFS.getManager
  val vfsModel = Vfs2(vfs2.VFS.getManager.resolveFile(VwJniModelTest.VwModelPath))
  val vfsSpec = Vfs2(vfs2.VFS.getManager.resolveFile("res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js"))
  val cds = ConstantDeltaSpline(0, 1, IndexedSeq(0.25, 0.75))
}

class VwJniModelJsonTest {
  import VwJniModelJsonTest._

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
             |    "via": "vfs2",
             |    "params": "--quiet -t",
             |    "model": """".stripMargin.trim +  base64EncodedModelString + """"
             |  }
             |}
           """).stripMargin.parseJson
      val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"))

      val fields = actual.asJsObject.fields
      val act = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))
      assertEquals(expected, act)
  }

  @Test def testGoodModelViaVfs1() = {
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
         |    "via": "vfs1",
         |    "params": "--quiet -t",
         |    "model": """".stripMargin.trim +  base64EncodedModelString + """"
                                                                             |  }
                                                                             |}
                                                                           """).stripMargin.parseJson

    val vfs1Model = Vfs1(vfs1.VFS.getManager.resolveFile(VwJniModelTest.VwModelPath))
    val actual = VwJniModel.json(vfsSpec, vfs1Model, ModelId(0, "model name"), Some("--quiet -t"))

    val fields = actual.asJsObject.fields
    val act = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))
    assertEquals(expected, act)
  }

  @Test def testGoodModelViaFile() = {
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
         |    "via": "file",
         |    "params": "--quiet -t",
         |    "model": """".stripMargin.trim +  base64EncodedModelString + """"
                                                                             |  }
                                                                             |}
                                                                           """).stripMargin.parseJson
    val vfsFile = File(new java.io.File(VwJniModelTest.VwModelPath))
    val actual = VwJniModel.json(vfsSpec, vfsFile, ModelId(0, "model name"), Some("--quiet -t"))

    val fields = actual.asJsObject.fields
    val act = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))
    assertEquals(expected, act)
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
             |    "via": "vfs2",
             |    "params": "--quiet -t",
             |    "model": """".stripMargin.trim + base64EncodedModelString + """"
             |  }
             |}
           """).stripMargin.parseJson
      val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), false, None, Some(Seq("This is a note")))

      val fields = actual.asJsObject.fields
      val act = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))
      assertEquals(expected, act)
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
             |    "via": "vfs2",
             |    "params": "--quiet -t",
             |    "model": """".stripMargin.trim + base64EncodedModelString + """"
             |  }
             |}
           """).stripMargin.parseJson

      val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), false, None, None, Some(cds))

      val fields = actual.asJsObject.fields
      val act = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))
      assertEquals(expected, act)
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
             |    "via": "vfs2",
             |    "params": "--quiet -t",
             |    "model": """".stripMargin.trim + base64EncodedModelString + """"
             |  }
             |}
           """).stripMargin.parseJson
      val actual = VwJniModel.json(vfsSpec, vfsModel, ModelId(0, "model name"), Some("--quiet -t"), false, None, Some(Seq("This is a note")), Some(cds))

      val fields = actual.asJsObject.fields
      val act = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))
      assertEquals(expected, act)
  }
}
