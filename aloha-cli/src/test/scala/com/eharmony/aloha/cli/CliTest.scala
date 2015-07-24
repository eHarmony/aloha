package com.eharmony.aloha.cli

import com.eharmony.aloha
import com.eharmony.matching.testhelp.io.{IoCaptureCompanion, TestWithIoCapture}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

object CliTest extends IoCaptureCompanion

/**
 * Created by rdeak on 6/16/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CliTest extends TestWithIoCapture(CliTest) {
    import CliTest._

    @Test def testNoArgs(): Unit = {
        Cli.main(Array.empty)
        assertEquals("No arguments supplied. Supply one of: '--dataset', '--modelrunner', '--vw'.", errContent.trim)
    }

    @Test def testBadFlag(): Unit = {
        Cli.main(Array("-BADFLAG"))
        assertEquals("'-BADFLAG' supplied. Supply one of: '--dataset', '--modelrunner', '--vw'.", errContent.trim)
    }

    @Test def testVw(): Unit = {
        Cli.main(Array("--vw"))
        val expected =
            """
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

        assertEquals(expected.trim, errContent.trim)
    }
}

