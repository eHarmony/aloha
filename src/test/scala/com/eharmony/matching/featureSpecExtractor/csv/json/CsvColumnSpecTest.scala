package com.eharmony.matching.featureSpecExtractor.csv.json

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json._


@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvColumnSpecTest {
    @Test def test1() {
        val examples = Seq(
            """{ "name": "long",       "type": "long",   "spec": "${long}" }""",
            """{ "name": "opt_double", "type": "double", "spec": "${opt_double}" }""",
            """{ "name": "syn_enum",   "type": "enum",   "spec": "${opt_string}", "values":    [ "e1v1" ] }""",
            """{ "name": "enum",       "type": "enum",   "spec": "${string}", "enumClass": "com.eharmony.matching.notaloha.AnEnum" }"""
        )

        val expected = Seq(
            LongCsvColumnSpec("long", "${long}"),
            DoubleCsvColumnSpec("opt_double", "${opt_double}"),
            SyntheticEnumCsvColumnSpec("syn_enum", "${opt_string}", Seq("e1v1")),
            EnumCsvColumnSpec("enum", "${string}", "com.eharmony.matching.notaloha.AnEnum")
        )

        val act = examples.foreach{ ex => CsvColumnSpec.csvColumnSpecFormat.read(ex.parseJson) }

        // TODO: actually test.
        // assertEquals(expected, act)
    }
}
