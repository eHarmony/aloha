package com.eharmony.aloha.ex

import org.junit.{Before, Test}
import org.junit.Assert._
import java.io.{PrintWriter, OutputStreamWriter, ByteArrayOutputStream, PrintStream}

class SchrodingerExceptionTest {

    /** Ensure a fresh exception for each test.
      */
    private[this] var ex: SchrodingerException = _

    @Before def before() {
        ex = new SchrodingerException
    }

    @Test def testFillInStackTrace() {
        assertTrue(new SchrodingerException().fillInStackTrace().isInstanceOf[SchrodingerException])
    }

    @Test(expected = classOf[SchrodingerException]) def testGetMessage() {
        ex.getMessage()
    }

    @Test(expected = classOf[SchrodingerException]) def testGetStackTrace() {
        ex.getStackTrace()
    }

    @Test(expected = classOf[SchrodingerException]) def testGetCause() {
        ex.getCause()
    }

    @Test(expected = classOf[SchrodingerException]) def testSetStackTrace() {
        ex.setStackTrace(Array.empty)
    }

    @Test(expected = classOf[SchrodingerException]) def testGetLocalizedMessage() {
        ex.getLocalizedMessage()
    }

    @Test(expected = classOf[SchrodingerException]) def testPrintStackTraceEmpty() {
        ex.printStackTrace()
    }

    @Test(expected = classOf[SchrodingerException]) def testPrintStackTraceStream() {
        val baos = new ByteArrayOutputStream()
        val ps = new PrintStream(baos)
        ex.printStackTrace(ps)
    }

    @Test(expected = classOf[SchrodingerException]) def testPrintStackTraceWriter() {
        val baos = new ByteArrayOutputStream()
        val osw = new OutputStreamWriter(baos)
        val ps = new PrintWriter(osw)
        ex.printStackTrace(ps)
    }

    @Test(expected = classOf[SchrodingerException]) def testInitCause() {
        ex.initCause(new Throwable)
    }

    @Test(expected = classOf[SchrodingerException]) def testToString() {
        ex.toString()
    }

    @Test def testNoThrowForSchrodingerExceptionWithSchrodingerExceptionCause() {
        new SchrodingerException(new SchrodingerException)
    }

    @Test def testNoThrowForSchrodingerExceptionWithExceptionCause() {
        new SchrodingerException(new Exception)
    }

    @Test(expected = classOf[SchrodingerException]) def testThrowForThrowableWithSchrodingerExceptionCause() {
        new Throwable(ex)
    }

    @Test(expected = classOf[SchrodingerException]) def testThrowForExceptionWithSchrodingerExceptionCause() {
        new Exception(ex)
    }

    @Test(expected = classOf[SchrodingerException]) def testThrowForRuntimeExceptionWithSchrodingerExceptionCause() {
        new RuntimeException(ex)
    }
}
