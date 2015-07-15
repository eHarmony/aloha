package com.eharmony.aloha.dataset.csv.json

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json._
import spray.json.DefaultJsonProtocol.{DoubleJsonFormat, LongJsonFormat}


@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvColumnTest {
    @Test def test1() {
        val examples = Seq(
            """{ "name": "long",       "type": "long",   "spec": "${long}" }""",
            """{ "name": "opt_double", "type": "double", "spec": "${opt_double}" }""",
            """{ "name": "syn_enum",   "type": "enum",   "spec": "${opt_string}", "values":    [ "e1v1" ] }""",
            """{ "name": "enum",       "type": "enum",   "spec": "${string}", "enumClass": "com.eharmony.matching.notaloha.AnEnum" }"""
        )

        val expected = Seq(
            CsvColumnWithDefault[Long]("long", "${long}"),
            CsvColumnWithDefault[Double]("opt_double", "${opt_double}"),
            SyntheticEnumCsvColumn("syn_enum", "${opt_string}", Seq("e1v1")),
            EnumCsvColumn("enum", "${string}", "com.eharmony.matching.notaloha.AnEnum")
        )

        val act = examples.foreach{ ex => CsvColumn.csvColumnSpecFormat.read(ex.parseJson) }

        // TODO: actually test.
        // assertEquals(expected, act)
    }
}
