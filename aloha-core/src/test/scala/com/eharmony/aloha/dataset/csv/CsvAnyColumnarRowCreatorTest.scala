package com.eharmony.aloha.dataset.csv

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.dataset.RowCreatorBuilder
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ryan.deak on 3/5/18.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class CsvAnyColumnarRowCreatorTest {
  import CsvAnyColumnarRowCreatorTest._

  @Test def test(): Unit = {
    val rc = specBuilder.fromString(spec("regular")).get
    val extract = (c: CsvLine) => rc.apply(c)._2.toVector
    val output = lines.map(extract)
    println(output)

  }
}

object CsvAnyColumnarRowCreatorTest {
  private def spec(encoding: String): String = {
    """
      |{
      |  "imports": [ "com.eharmony.aloha.feature.BasicFunctions._" ],
      |  "nullValue": "null",
      |  "encoding": "ENCODING_VALUE",
      |  "features": [
      |    { "spec": "1 to ${long}.toInt padTo (4, 0)", "type": "int", "size": 4, "name": "int_seq" },
      |    { "spec": "${opt_double}", "type": "double", "name": "double" },
      |    { "spec": "${opt_string}", "type": "string", "optional": true, "name": "opt_str" },
      |    { "spec": "${string}", "type": "enum", "values": ["s_2_1", "s_2_2", "s_2_3", "s_2_4"], "name": "enum" },
      |    { "spec": "${string1}.split(\":\").padTo(3, \"PAD\")", "type": "string", "size": 3, "name": "string" }
      |  ]
      |}
    """.stripMargin.replaceAllLiterally("ENCODING_VALUE", encoding)
  }

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
      """1,,,s_2_1,s_3_1""",
      """2,10.0,,s_2_2,s_3_2""",
      """3,11.0,s_1_3,s_2_3,s_3_3""",
      """4,12.0,s_1_4,s_2_4,one:two:three"""
    )

    val sb = RowCreatorBuilder(semantics, List(CsvAnyColumnarRowCreator.Producer[CsvLine]()))
    (lines, sb)
  }
}
