package com.eharmony.aloha.dataset.csv.json

import com.eharmony.aloha.reflect.{RefInfo, RefInfoOps}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json._
import spray.json.DefaultJsonProtocol._


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

        val act = examples.map { ex => CsvColumn.csvColumnSpecFormat.read(ex.parseJson) }

        assertEquals(expected, act)
    }

//    final case class SyntheticEnumCsvColumn(name: String, spec: String, values: Seq[String], defVal: Option[String] = None)

    @Test def testReqEnum() {
        val jsonTxt = """{ "name": "some_enum",
                        |  "type": "enum",
                        |  "spec": "${string}",
                        |  "enumClass": "com.eharmony.matching.notaloha.AnEnum"
                        |}""".stripMargin
        val json = jsonTxt.parseJson
        val col = json.convertTo[CsvColumn]
        assertTrue(col.isInstanceOf[EnumCsvColumn])
    }

    @Test def testOptEnum() {
        val jsonTxt = """{ "name": "some_enum",
                        |  "type": "enum",
                        |  "spec": "${string}",
                        |  "enumClass": "com.eharmony.matching.notaloha.AnEnum",
                        |  "defVal": "VALUE_2",
                        |  "optional": true
                        |}""".stripMargin
        val json = jsonTxt.parseJson
        val col = json.convertTo[CsvColumn]
        assertTrue(col.isInstanceOf[OptionEnumCsvColumn[_]])
    }

    @Test def testSizedByte(): Unit = testSizedCreation[Byte]
    @Test def testSizedChar(): Unit = testSizedCreation[Char]
    @Test def testSizedShort(): Unit = testSizedCreation[Short]
    @Test def testSizedInt(): Unit = testSizedCreation[Int]
    @Test def testSizedLong(): Unit = testSizedCreation[Long]
    @Test def testSizedFloat(): Unit = testSizedCreation[Float]
    @Test def testSizedDouble(): Unit = testSizedCreation[Double]
    @Test def testSizedString(): Unit = testSizedCreation[String]

    private def testSizedCreation[A: RefInfo: JsonFormat]: Unit = {
        val tpe = RefInfoOps.toString(RefInfo[A]).split("\\.").last.toLowerCase
        val name = tpe.replaceAll("[aeiou]", "")
        val spec = "${string}"
        val jsonTxt = s"""{ "name": "$name", "type": "$tpe", "size": 2, "spec": "$spec"}"""
        val col = jsonTxt.parseJson.convertTo[CsvColumn]
        val exp = SeqCsvColumnWithNoDefault[A](name, spec, 2)
        assertEquals(exp, col)
    }
}