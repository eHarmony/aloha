package com.eharmony.aloha.cli

import com.eharmony.aloha.annotate.CLI
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
 * Created by rdeak on 6/16/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class DuplicateCliFlagTest {
    @Test def testNoFlagDuplicates(): Unit = {
        val flagClassMap = Cli.cliClasses.toVector.map { c => c.getAnnotation(classOf[CLI]).flag() -> c }
        val repeatedFlags = flagClassMap.groupBy(_._1).mapValues(_.map(_._2.getCanonicalName)).filter(1 < _._2.size)
        assertEquals("No flags should be repeated.  Offenders: ", Map.empty[String, Class[Any]], repeatedFlags)
    }
}
