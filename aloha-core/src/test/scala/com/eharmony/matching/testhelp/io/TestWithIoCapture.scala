package com.eharmony.matching.testhelp.io

import java.io.{ByteArrayOutputStream, PrintStream}

import org.junit.{After, Before}

import scala.util.Try

/**
 * Exposes a way to capture and read data that goes to stdout and stderr.
 * See [[IoCaptureCompanion]] for usage.
 * @author R M Deak
 */
abstract class TestWithIoCapture[C <: IoCaptureCompanion](companion: C) {
//    @Before def before(): Unit = {
//        companion.setStdErr()
//        companion.setStdOut()
//    }
//
//    @After def after(): Unit = {
//        companion.clearStdErr()
//        companion.clearStdOut()
//    }


    protected[this] case class InterrogatablePrintStream(baos: ByteArrayOutputStream) extends PrintStream(baos) {
        def output: String = {
            baos.flush()
            new String(baos.toByteArray)
        }
    }

    protected[this] implicit def interrogatablePrintStream: InterrogatablePrintStream =
        InterrogatablePrintStream(new ByteArrayOutputStream)

    protected[this] def runMain[A <: PrintStream](mainFn: Array[String] => Unit, args: Array[String])
                                                 (implicit out: A, err: A): (A, A) = {

        val t = Console.withOut(out) {
            Console.withErr(err) {
                Try {
                    mainFn(args)
                    (out, err)
                }
            }
        }

        t.get

        // TODO: Remove
//        val (cOut, cErr) = (Console.out, Console.err)
//
//        Console.setOut(out)
//        Console.setErr(err)
//
//        try {
//            mainFn(args)
//            (out, err)
//        }
//        finally {
//            Console.setOut(cOut)
//            Console.setErr(cErr)
//        }
    }
}
