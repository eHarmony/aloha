package com.eharmony.matching.aloha.cli

import com.eharmony.matching.aloha
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
              |        spec is an Apache VFS URL to an aloha spec file with modelType 'VwJNI'.
              |  -m <value> | --model <value>
              |        model is an Apache VFS URL to a VW binary model.
            """.stripMargin
        assertEquals(expected.trim, errContent.trim)
    }
}

