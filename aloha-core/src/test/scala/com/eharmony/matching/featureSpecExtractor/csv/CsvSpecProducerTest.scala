package com.eharmony.matching.featureSpecExtractor.csv

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import com.eharmony.matching.featureSpecExtractor.{MissingAndErroneousFeatureInfo, SpecBuilder}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvSpecProducerTest {
    import CsvSpecProducerTest._

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
            (MissingAndErroneousFeatureInfo(List(),List()),                              "2.0,1,2.0,e1v1,e2v1,VALUE_2"),
            (MissingAndErroneousFeatureInfo(List("default_field", "opt_double"),List()), "null,1,null,null,e2v1,VALUE_2"),
            (MissingAndErroneousFeatureInfo(List(),List()),                              "2.0,1,2.0,null,null,VALUE_2"),
            (MissingAndErroneousFeatureInfo(List(),List()),                              "2.0,1,2.0,e1v1,null,VALUE_3")
        )

        val actual = lines.map(spec.apply)

        assertEquals(expected, actual)
    }
}

object CsvSpecProducerTest {

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

        val sb = SpecBuilder(semantics, List(CsvSpecProducer[CsvLine]()))
        (lines, sb)
    }
}
