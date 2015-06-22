package com.eharmony.matching.aloha

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner


@RunWith(classOf[BlockJUnit4ClassRunner])
class PkgTest {
    @Test def testPkgLocationOk(): Unit = {
        assertEquals("com.eharmony.matching.aloha", pkgName)
    }

    /**
     * Test that we can get the version and that it is a proper format.
     */
    @Test def testVersionFormatOk(): Unit = {
        val ok = """(\d+)\.(\d+)\.(\d+)(-(SNAPSHOT))?""".r
        version match {
            case ok(major, minor, fix, _, snapshot) => ()
            case notOk => fail(s"Bad version format: $notOk")
        }
    }
}
