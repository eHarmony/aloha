package com.eharmony.aloha.semantics.compiled.plugin.csv

import org.junit.Test
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Assert._

/** Test that the functions in CsvLine are the same as what's in CsvTypes and the proper number of types exist.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvTypesTest {
    @Test def testNumTypesCorrect() {
        assertEquals("Wrong number of types found in CsvTypes", 28, CsvTypes.values.size)
    }

    @Test def testTypeMethodCorrespondence() {
        val typeNames = CsvTypes.values.map(_.toString).toSet
        val methodNames = classOf[CsvLine].getDeclaredMethods.map(_.getName).toSet

        val typesWithoutMethods = typeNames -- methodNames
        val methodsWithoutTypes = methodNames -- typeNames

        assertEquals(s"The following types in CsvTypes seem to be missing methods in CsvLine: ${typesWithoutMethods.mkString("{", ", ", "}" )}", 0, typesWithoutMethods.size)
        assertEquals(s"The following methods in CsvLine don't seem to have associated types in CsvTypes: ${methodsWithoutTypes.mkString("{", ", ", "}" )}", 0, methodsWithoutTypes.size)
    }
}
