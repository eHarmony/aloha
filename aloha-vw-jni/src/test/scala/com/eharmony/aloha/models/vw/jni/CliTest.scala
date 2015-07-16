package com.eharmony.aloha.models.vw.jni

import java.io.FileInputStream

import com.eharmony.aloha
import com.eharmony.matching.testhelp.io.{IoCaptureCompanion, TestWithIoCapture}
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{BeforeClass, Test}
import spray.json.{DeserializationException, pimpString}

object CliTest extends IoCaptureCompanion {
    @BeforeClass def createModel(): Unit = VwJniModelTest.createModel()
    lazy val base64EncodedModelString = Option(VwJniModelTest.VwModelFile.exists).collect{case true => VwJniModel.readBinaryVwModelToB64String(new FileInputStream(VwJniModelTest.VwModelFile))}
}

/**
 * These tests are now designed to pass if the VW model cannot be created in the BeforeClass method.
 * This is due to Travis not working as we expect it to.  Because cat /proc/version doesn't match
 * the purported os the VW JNI library doesn't know which system dependent version of the lib
 * to load and these tests will consequently fail.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CliTest extends TestWithIoCapture(CliTest) {
    import CliTest._

    @Test def testMissingBothParams(): Unit = {
        Cli.main(Array.empty)
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
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
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

        assertEquals(expected.trim, errContent.trim)
    }

    @Test def testMissingSpecParam(): Unit = {
        Cli.main(Array("-m", VwJniModelTest.VwModelPath))
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
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
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

        assertEquals(expected.trim, errContent.trim)
    }

    @Test def testMissingModelParam(): Unit = {
        Cli.main(Array("-s", "res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js"))
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
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
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

        assertEquals(expected.trim, errContent.trim)
    }

    @Test def testSpecFileDoesntExist(): Unit = {
        Cli.main(Array("-m", VwJniModelTest.VwModelPath, "-s", "res:SPECTHATDOESNTEXIST"))
        val expected =
            """
              |Error: Option --spec failed when given 'res:SPECTHATDOESNTEXIST'. Badly formed URI "res:SPECTHATDOESNTEXIST".
              |vw """.stripMargin + aloha.version + """
              |Usage: vw [options]
              |
              |  -s <value> | --spec <value>
              |        spec is an Apache VFS URL to an aloha spec file.
              |  -m <value> | --model <value>
              |        model is an Apache VFS URL to a VW binary model.
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
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

        assertEquals(expected.trim, errContent.trim)
    }

    @Test def testModelFileDoesntExist(): Unit = {
        Cli.main(Array("-m", "res:SPECTHATDOESNTEXIST", "-s", "res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js"))
        val expected =
            """
              |Error: Option --model failed when given 'res:SPECTHATDOESNTEXIST'. Badly formed URI "res:SPECTHATDOESNTEXIST".
              |vw """.stripMargin + aloha.version + """
              |Usage: vw [options]
              |
              |  -s <value> | --spec <value>
              |        spec is an Apache VFS URL to an aloha spec file.
              |  -m <value> | --model <value>
              |        model is an Apache VFS URL to a VW binary model.
              |  -n <value> | --name <value>
              |        name of the model.
              |  -i <value> | --id <value>
              |        numeric id of the model.
              |  --vw-args <value>
              |        arguments to vw
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

        assertEquals(expected.trim, errContent.trim)
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

    @Test def testHappy(): Unit = {
        if (VwJniModelTest.VwModelFile.exists) {
            val args = Array(
                "-m", VwJniModelTest.VwModelPath,
                "-s", "res:com/eharmony/aloha/models/vw/jni/good.logistic.aloha.js",
                "-i", "0",
                "-n", "model name",
                "--vw-args", "--quiet -t"
            )

            Cli.main(args)

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
                   |    "model": """".stripMargin.trim + base64EncodedModelString.get + """"
                   |  }
                   |}
                 """).stripMargin.parseJson

            assertEquals(expected, outContent.parseJson)
        }
    }
}

