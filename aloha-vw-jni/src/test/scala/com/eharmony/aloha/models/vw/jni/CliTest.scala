package com.eharmony.aloha.models.vw.jni

import java.io.{File, FileInputStream}

import com.eharmony.aloha
import com.eharmony.aloha.util.io.TestWithIoCapture
import org.apache.commons.vfs2
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{BeforeClass, Test}
import spray.json.{DeserializationException, JsObject, pimpString}
import vw.learner.{VWIntLearner, VWLearners}

object CliTest {
  @BeforeClass def createModel(): Unit = VwJniModelTest.createModel()

  lazy val base64EncodedModelString = VwJniModel.readBinaryVwModelToB64String(new FileInputStream(VwJniModelTest.VwModelFile))

  private[jni] lazy val cbVwModelPath = {
    val tf = File.createTempFile("vwcb_", ".model")
    tf.deleteOnExit()
    val p = tf.getCanonicalPath

    val vw = VWLearners.create[VWIntLearner](s"--cb 2 --quiet -f $p")
    val input = Vector("1:2:0.5 | a c",
      "2:1:0.5 | b c")
    for {
      i <- 1 to 100
      example <- input
    } vw.learn(example)
    vw.close()
    p
  }
}

/**
 * These tests are now designed to pass if the VW model cannot be created in the BeforeClass method.
 * This is due to Travis not working as we expect it to.  Because cat /proc/version doesn't match
 * the purported os the VW JNI library doesn't know which system dependent version of the lib
 * to load and these tests will consequently fail.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CliTest extends TestWithIoCapture { // (CliTest) {
    import CliTest._

    @Test def testMissingBothParams(): Unit = {
      val res = run(Cli.main)(Array.empty)
      val expected =
            ("""
              |Error: Missing option --spec
              |Error: Missing option --model
              |vw """.stripMargin + aloha.version + """
              |Usage: vw [options]
              |
              |  -s <value> | --spec <value>
              |        spec is an Apache VFS URL to an aloha spec file.
              |  -m <value> | --model <value>
              |        model is an Apache VFS URL to a VW binary model.
              |  --fs-type <value>
              |        file system type: vfs1, vfs2, file. default = vfs2.
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
              |  --external
              |        link to a binary VW model rather than embedding it inline in the aloha model.
              |  --num-missing-thresh <value>
              |        number of missing features to allow before returning a 'no-prediction'.
              |  --note <value>
              |        notes to add to the model. Can provide this many parameter times.
              |  --spline-min <value>
              |        min value for spline domain. (must additional provide spline-max and spline-knots).
              |  --spline-max <value>
              |        max value for spline domain. (must additional provide spline-min and spline-knots).
              |  --spline-knots <value>
              |        max value for spline domain. (must additional provide spline-min, spline-delta, and spline-knots).
            """).stripMargin

      assertEquals(expected.trim, res.err.contents.trim)
    }

    @Test def testMissingSpecParam(): Unit = {
      val args = Array("-m", VwJniModelTest.VwModelPath)
      val res = run(Cli.main)(args)

      val expected =
            """
              |Error: Missing option --spec
              |vw """.stripMargin + aloha.version + """
              |Usage: vw [options]
              |
              |  -s <value> | --spec <value>
              |        spec is an Apache VFS URL to an aloha spec file.
              |  -m <value> | --model <value>
              |        model is an Apache VFS URL to a VW binary model.
              |  --fs-type <value>
              |        file system type: vfs1, vfs2, file. default = vfs2.
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
              |  --external
              |        link to a binary VW model rather than embedding it inline in the aloha model.
              |  --num-missing-thresh <value>
              |        number of missing features to allow before returning a 'no-prediction'.
              |  --note <value>
              |        notes to add to the model. Can provide this many parameter times.
              |  --spline-min <value>
              |        min value for spline domain. (must additional provide spline-max and spline-knots).
              |  --spline-max <value>
              |        max value for spline domain. (must additional provide spline-min and spline-knots).
              |  --spline-knots <value>
              |        max value for spline domain. (must additional provide spline-min, spline-delta, and spline-knots).
            """.stripMargin

      assertEquals(expected.trim, res.err.contents.trim)
    }

    @Test def testMissingModelParam(): Unit = {
      val args = Array("-s", "res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js")
      val res = run(Cli.main)(args)

      val expected =
            """
              |Error: Missing option --model
              |vw """.stripMargin + aloha.version + """
              |Usage: vw [options]
              |
              |  -s <value> | --spec <value>
              |        spec is an Apache VFS URL to an aloha spec file.
              |  -m <value> | --model <value>
              |        model is an Apache VFS URL to a VW binary model.
              |  --fs-type <value>
              |        file system type: vfs1, vfs2, file. default = vfs2.
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
              |  --external
              |        link to a binary VW model rather than embedding it inline in the aloha model.
              |  --num-missing-thresh <value>
              |        number of missing features to allow before returning a 'no-prediction'.
              |  --note <value>
              |        notes to add to the model. Can provide this many parameter times.
              |  --spline-min <value>
              |        min value for spline domain. (must additional provide spline-max and spline-knots).
              |  --spline-max <value>
              |        max value for spline domain. (must additional provide spline-min and spline-knots).
              |  --spline-knots <value>
              |        max value for spline domain. (must additional provide spline-min, spline-delta, and spline-knots).
            """.stripMargin

      assertEquals(expected.trim, res.err.contents.trim)
    }

    @Test def testSpecFileDoesntExist(): Unit = {
        try {
            Cli.main(Array("-m", VwJniModelTest.VwModelPath, "-s", "res:SPECTHATDOESNTEXIST"))
        }
        catch {
            case e: vfs2.FileSystemException => assertEquals("Badly formed URI \"res:SPECTHATDOESNTEXIST\".", e.getMessage)
        }
    }

    @Test def testModelFileDoesntExist(): Unit = {
        try {
            Cli.main(Array("-m", "res:SPECTHATDOESNTEXIST", "-s", "res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js"))
        }
        catch {
            case e: vfs2.FileSystemException => assertEquals("Badly formed URI \"res:SPECTHATDOESNTEXIST\".", e.getMessage)
        }
    }

    @Test def testArrayJson(): Unit = {
        try {
            Cli.main(Array("-m", VwJniModelTest.VwModelPath,
                           "-s", "res:com/eharmony/aloha/models/vw/jni/array.js"))
        }
        catch {
            case e: DeserializationException if e.getMessage == "Object expected in field 'imports'" =>
            case e: Throwable => throw e
        }
    }

    @Test def testHappyEmbedded(): Unit = {
        if (VwJniModelTest.VwModelFile.exists) {
            val args = Array(
                "-m", VwJniModelTest.VwModelPath,
                "-s", "res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js",
                "-i", "0",
                "-n", "model name",
                "--vw-args", "--quiet -t"
            )

            val res = run(Cli.main)(args)

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

            val fields = res.out.contents.parseJson.asJsObject.fields
            val actual = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))

            assertEquals(expected, actual)
        }
    }

    @Test def testHappyExternal(): Unit = {
        if (VwJniModelTest.VwModelFile.exists) {
            val args = Array(
                "-m", VwJniModelTest.VwModelPath,
                "-s", "res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js",
                "-i", "0",
                "-n", "model name",
                "--vw-args", "--quiet -t",
                "--external"
            )

            val res = run(Cli.main)(args)

            val url = vfs2.VFS.getManager.resolveFile(VwJniModelTest.VwModelPath)

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
                   |    "modelUrl": """".stripMargin.trim + url.getName.toString + """",
                   |    "via": "vfs2"
                   |  }
                   |}
                 """).stripMargin.parseJson

            val fields = res.out.contents.parseJson.asJsObject.fields
            val actual = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))

            assertEquals(expected, actual)
        }
    }


  /**
   * Tests whether classLabels array in spec goes into the model JSON.
   */
  @Test def testHappyExternalCb(): Unit = {
    val args = Array(
      "-m", CliTest.cbVwModelPath,
      "-s", "res:com/eharmony/aloha/models/vw/jni/good.cb.aloha.js",
      "-i", "0",
      "-n", "model name",
      "--vw-args", "--quiet -t",
      "--external"
    )

    val res = run(Cli.main)(args)

    val url = vfs2.VFS.getManager.resolveFile(CliTest.cbVwModelPath)

    val expected =
      ("""
         |{
         |  "modelType": "VwJNI",
         |  "modelId": { "id": 0, "name": "model name" },
         |  "features": {
         |    "b": "Seq((\"\", 1.0))",
         |    "c": "Seq((\"\", 1.0))"
         |  },
         |  "namespaces": {},
         |  "vw": {
         |    "params": "--quiet -t",
         |    "modelUrl": """".stripMargin.trim + url.getName.toString + """",
         |    "via": "vfs2"
         |  },
         |  "classLabels": [ "Career", "Family" ]
         |}
       """).stripMargin.parseJson

    val fields = res.out.contents.parseJson.asJsObject.fields
    val actual = JsObject(fields + ("vw" -> JsObject(fields("vw").asJsObject.fields - "creationDate")))

    assertEquals(expected, actual)
  }
}

