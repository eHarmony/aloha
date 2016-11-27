package com.eharmony.matching.testhelp.io

import java.io._

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
    // TODO: Uncomment
//    private[this] val stdOut = System.out
//    private[this] val stdErr = System.err
//    private[this] val stdIn = System.in
//    def originalStdIn = stdIn

//    trait ProxiedInputStream extends InputStream with Closeable {
//        protected[this] val is: InputStream
//        override def read() = is.read()
//        override def read(a: Array[Byte]): Int = is.read(a)
//        override def read(a: Array[Byte], offset: Int, len: Int): Int = is.read(a, offset, len)
//        override def skip(v: Long) = is.skip(v)
//        override def available() = is.available()
//        override def mark(v: Int) = is.mark(v)
//        override def reset() = is.reset()
//        override def markSupported() = is.markSupported()
//        override def close() = is.close()
//    }
//
//    class StringBufferInputStream extends ProxiedInputStream {
//        val buffer = new StringBuffer
//        protected[this] val is = new ReaderInputStream(new CharSequenceReader(buffer))
//    }
//private[this] val stdinBais = new ThreadLocal[InputStream] {
//    override protected[this] def initialValue() = {
//        val b = new StringBuffer()
//        new ReaderInputStream(new CharSequenceReader(b))
//    }
//}
//
//  private[io] def setStdIn(): Unit = System.setIn(new PrintStream(stdoutBaos.get))


// TODO: Uncomment
//    private[this] val stderrBaos = new ThreadLocal[ByteArrayOutputStream] {
//        override protected[this] def initialValue() = new ByteArrayOutputStream
//    }
//
//    private[this] val stdoutBaos = new ThreadLocal[ByteArrayOutputStream] {
//        override protected[this] def initialValue() = new ByteArrayOutputStream
//    }
//
//    private[io] def setStdOut(): Unit = System.setOut(new PrintStream(stdoutBaos.get))
//    private[io] def setStdErr(): Unit = System.setErr(new PrintStream(stderrBaos.get))
//    def clearStdOut(): Unit = stdoutBaos.get.reset()
//    def clearStdErr(): Unit = stderrBaos.get.reset()
//
//
//    def errContent = stderrBaos.get.toString
//    def outContent = stdoutBaos.get.toString
//
//    @AfterClass def afterClass(): Unit = {
//        System.setErr(stdErr)
//        System.setOut(stdOut)
//    }
}
