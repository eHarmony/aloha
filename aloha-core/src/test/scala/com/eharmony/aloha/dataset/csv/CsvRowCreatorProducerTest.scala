package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.dataset.{MissingAndErroneousFeatureInfo, RowCreatorBuilder}
import com.eharmony.aloha.semantics.SemanticsUdfException
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv._
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvRowCreatorProducerTest {

    import CsvRowCreatorProducerTest._

    @Test def test1() {
        val json =
            """
              |{
              |  "imports": [],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    {
              |      "name": "default_field",
              |      "spec": "${opt_double}"
              |    },
              |    {
              |      "name": "long",
              |      "type": "long",
              |      "spec": "${long}"
              |    },
              |    {
              |      "name": "opt_double",
              |      "type": "double",
              |      "spec": "${opt_double}"
              |    },
              |    {
              |      "name": "syn_enum",
              |      "type": "enum",
              |      "values": [ "e1v1" ],
              |      "spec": "${opt_string}"
              |    },
              |    {
              |      "name": "syn_enum1",
              |      "type": "enum",
              |      "values": [ "e2v1" ],
              |      "spec": "${string}"
              |    },
              |    {
              |      "name": "enum",
              |      "type": "enum",
              |      "enumClass": "com.eharmony.matching.notaloha.AnEnum",
              |      "spec": "com.eharmony.matching.notaloha.AnEnum.valueOf(${string1})"
              |    }
              |  ]
              |}
            """.stripMargin.trim

        val spec = specBuilder.fromString(json).get

        val expected = Seq(
            (MissingAndErroneousFeatureInfo(List(), List()),
             "2.0,1,2.0,e1v1,e2v1,VALUE_2"),
            (MissingAndErroneousFeatureInfo(List("default_field", "opt_double", "syn_enum"), List()),
             "null,1,null,null,e2v1,VALUE_2"),
            (MissingAndErroneousFeatureInfo(List(), List()),
             "2.0,1,2.0,null,null,VALUE_2"),
            (MissingAndErroneousFeatureInfo(List(), List()),
             "2.0,1,2.0,e1v1,null,VALUE_3")
        )

        val actual = lines.map(spec)

        assertEquals(expected, actual)
    }

    @Test def testVectorizedInput(): Unit = {
        val line = (1 to 10).mkString(",") + "," + (11 to 20).map(_.toDouble).mkString(",")

        test(
            """
              |{
              |  "imports": [],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "spec": "1 to 10", "type": "int", "size": 10, "name": "i_1_10" },
              |    { "spec": "11 to 20 map (_.toDouble)", "type": "double", "size": 10, "name": "d_1_10" }
              |  ]
              |}
            """.stripMargin,

            Seq.fill(4)(line).mkString("\n")
        )
    }

    @Test def testWrongSizedOutput(): Unit = {

        val json =
            """
              |{
              |  "imports": [],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "spec": "1 to 3", "type": "int", "size": 2, "name": "i=1..3, size=2" }
              |  ]
              |}
            """.stripMargin

        val spec = specBuilder.fromString(json).get
        val pipeline = lines.view.take(1).map(spec)
        val expMsg = "requirement failed: feature 'i=1..3, size=2' output size != 2"

        try {
            pipeline.headOption
            fail()
        }
        catch {
            case SemanticsUdfException(_, accOut, accMissingOut, accErr, cause, _) =>
                assertTrue(cause.isInstanceOf[IllegalArgumentException])
                assertEquals(expMsg, cause.getMessage)
                assertTrue(accOut.isEmpty)
                assertTrue(accMissingOut.isEmpty)
                assertTrue(accErr.isEmpty)
            case _ => fail()
        }
    }


    // Issue 98:  Test that when optional flag is provided, the proper values are returned whether

    @Test def testOptionalOutputWithNoVars() {
        test(
            """
              |{
              |  "imports": [],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "optional": true, "spec": "Some(1)", "defVal": -1, "type": "int", "name": "n_s_y" },
              |    { "optional": true, "spec": "Some(1)",               "type": "int", "name": "n_s_n" },
              |    { "optional": true, "spec": "None",    "defVal": -1, "type": "int", "name": "n_n_y" },
              |    { "optional": true, "spec": "None",                  "type": "int", "name": "n_n_n" }
              |  ]
              |}
            """.stripMargin,

            """
              |1, 1, -1, null
              |1, 1, -1, null
              |1, 1, -1, null
              |1, 1, -1, null
            """.stripMargin
        )
    }

    @Test def testOptionalOutputWithOptVars() {
        test(
            """
              |{
              |  "imports": [],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "optional": true, "spec": "Some(${opt_double})",           "defVal": -1, "type": "double", "name": "o_s_y" },
              |    { "optional": true, "spec": "Some(${opt_double})",                         "type": "double", "name": "o_s_n" },
              |    { "optional": true, "spec": "None map (_ => ${opt_double})", "defVal": -1, "type": "double", "name": "o_n_y" },
              |    { "optional": true, "spec": "None map (_ => ${opt_double})",               "type": "double", "name": "o_n_n" }
              |  ]
              |}
            """.stripMargin,

            """
              | 2.0, 2.0,  -1.0, null
              |-1.0, null, -1.0, null
              | 2.0, 2.0,  -1.0, null
              | 2.0, 2.0,  -1.0, null
            """.stripMargin
        )
    }

    @Test def testOptionalOutputWithReqVars() {
        test(
            """
              |{
              |  "imports": [],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "optional": true, "spec": "Some(${long})",           "defVal": -1, "type": "long", "name": "r_s_y" },
              |    { "optional": true, "spec": "Some(${long})",                         "type": "long", "name": "r_s_n" },
              |    { "optional": true, "spec": "None map (_ => ${long})", "defVal": -1, "type": "long", "name": "r_n_y" },
              |    { "optional": true, "spec": "None map (_ => ${long})",               "type": "long", "name": "r_n_n" }
              |  ]
              |}
            """.stripMargin,

            """
              |1, 1, -1, null
              |1, 1, -1, null
              |1, 1, -1, null
              |1, 1, -1, null
            """.stripMargin
        )
    }

    @Test def testOptionalEnumOutputWithNoVars() {

        test(
            """
              |{
              |  "imports": [ "com.eharmony.matching.notaloha.AnEnum" ],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "optional": true, "spec": "Some(AnEnum.VALUE_2)",           "defVal": "VALUE_3", "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_s_y" },
              |    { "optional": true, "spec": "Some(AnEnum.VALUE_2)",                                "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_s_n" },
              |    { "optional": true, "spec": "None map (_ => AnEnum.VALUE_2)", "defVal": "VALUE_3", "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_n_y" },
              |    { "optional": true, "spec": "None map (_ => AnEnum.VALUE_2)",                      "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_n_n" }
              |  ]
              |}
            """.stripMargin,

            """
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
            """.stripMargin
        )
    }

    @Test def testOptionalEnumOutputWithReqVars() {
        test(
            """
              |{
              |  "imports": [ "com.eharmony.matching.notaloha.AnEnum" ],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "optional": true, "spec": "Some(${long}) map (_ => AnEnum.VALUE_2)", "defVal": "VALUE_3", "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_s_y" },
              |    { "optional": true, "spec": "Some(${long}) map (_ => AnEnum.VALUE_2)",                      "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_s_n" },
              |    { "optional": true, "spec": "Some(${long}) flatMap (_ => None)",       "defVal": "VALUE_3", "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_n_y" },
              |    { "optional": true, "spec": "Some(${long}) flatMap (_ => None)",                            "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_n_n" }
              |  ]
              |}
            """.stripMargin,

            """
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
            """.stripMargin
        )
    }

    @Test def testOptionalEnumOutputWithOptVars() {
        test(
            """
              |{
              |  "imports": [ "com.eharmony.matching.notaloha.AnEnum" ],
              |  "separator": ",",
              |  "nullValue": "null",
              |  "features": [
              |    { "optional": true, "spec": "Some(${opt_double}) map (i => AnEnum.values()(i.toInt - 2))", "defVal": "VALUE_3", "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_s_y" },
              |    { "optional": true, "spec": "Some(${opt_double}) map (i => AnEnum.values()(i.toInt - 2))",                      "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_s_n" },
              |    { "optional": true, "spec": "Some(${opt_double}) flatMap (_ => None)",                     "defVal": "VALUE_3", "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_n_y" },
              |    { "optional": true, "spec": "Some(${opt_double}) flatMap (_ => None)",                                          "type": "enum", "enumClass": "com.eharmony.matching.notaloha.AnEnum", "name": "o_n_n" }
              |  ]
              |}
            """.stripMargin,

            """
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_3, null,    VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
              |VALUE_2, VALUE_2, VALUE_3, null
            """.stripMargin
        )
    }

    def test(json: String, expected: String) = {
        val exp = expected.trim.filterNot(' ' ==).split("\n").toVector
        val spec = specBuilder.fromString(json).get
        val actual = lines.map(spec).map(_._2.toString)
        assertEquals(exp, actual)
    }
}

object CsvRowCreatorProducerTest {

    private lazy val (lines, specBuilder) = {
        val types = Seq(
            "long" -> CsvTypes.LongType,
            "opt_double" -> CsvTypes.DoubleOptionType,
            "opt_string" -> CsvTypes.StringOptionType,
            "string" -> CsvTypes.StringType,
            "string1" -> CsvTypes.StringType
        )

        val plugin = CompiledSemanticsCsvPlugin(types.toMap)
        val semantics = CompiledSemantics(TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)), plugin, Nil)
        val csvLines = CsvLines(indices = types.unzip._1.zipWithIndex.toMap, fs = ",")

        val lines = csvLines(
            """1,2.0,e1v1,e2v1,VALUE_2""",
            """1,,,e2v1,VALUE_2""",
            """1,2.0,e1v2,e2v2,VALUE_2""",
            """1,2.0,e1v1,e2v2,VALUE_3"""
        )

        val sb = RowCreatorBuilder(semantics, List(CsvRowCreator.Producer[CsvLine]()))
        (lines, sb)
    }
}
