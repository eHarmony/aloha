package com.eharmony.matching.testhelp.io

import org.junit.{After, Before}

/**
 * Exposes a way to capture and read data that goes to stdout and stderr.
 * See [[IoCaptureCompanion]] for usage.
 * @author R M Deak
 */
abstract class TestWithIoCapture[C <: IoCaptureCompanion](companion: C) {
    @Before def before(): Unit = {
        companion.setStdErr()
        companion.setStdOut()
    }

    @After def after(): Unit = {
        companion.clearStdErr()
        companion.clearStdOut()
    }
}
