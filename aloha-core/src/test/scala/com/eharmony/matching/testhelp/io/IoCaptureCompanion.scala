package com.eharmony.matching.testhelp.io

import java.io.{ByteArrayOutputStream, PrintStream}

import org.junit.AfterClass

/**
 * Exposes a way to capture and read data that goes to stdout and stderr.  To read, use ''errContent'' and
 * ''outContent'' methods.
 *
 * '''TO USE''': Make a companion object extend this trait.  Place the companion ''ABOVE'' the test class.  Then
 * have the test class extend the [[TestWithIoCapture]] class.  E.g.:
 * {{{
 * import com.eharmony.matching.testhelp.io._
 * import org.junit.Assert.assertEquals
 *
 * object Test extends IoCaptureCompanion
 * class Test extends TestWithIoCapture(Test) {
 *   import Test._
 *
 *   @Test def test(): Unit = {
 *     val output = "xyz"
 *     println("xyz")
 *     assertEquals(output, outContent.trim)
 *   }
 * }
 * }}}
 *
 * @author R M Deak
 */
trait IoCaptureCompanion {
    private[this] val stdOut = System.out
    private[this] val stdErr = System.err

    private[this] val stderrBaos = new ThreadLocal[ByteArrayOutputStream] {
        override protected[this] def initialValue() = new ByteArrayOutputStream
    }

    private[this] val stdoutBaos = new ThreadLocal[ByteArrayOutputStream] {
        override protected[this] def initialValue() = new ByteArrayOutputStream
    }

    private[io] def setStdOut(): Unit = System.setOut(new PrintStream(stdoutBaos.get))
    private[io] def setStdErr(): Unit = System.setErr(new PrintStream(stderrBaos.get))
    def clearStdOut(): Unit = stdoutBaos.get.reset()
    def clearStdErr(): Unit = stderrBaos.get.reset()

    def errContent = stderrBaos.get.toString
    def outContent = stdoutBaos.get.toString

    @AfterClass def afterClass(): Unit = {
        System.setErr(stdErr)
        System.setOut(stdOut)
    }
}
