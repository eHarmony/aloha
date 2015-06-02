package com.eharmony.matching.aloha.models.vw.jni

import com.eharmony.matching.aloha.factory.ModelFactory
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CsvLines, CsvTypes, CompiledSemanticsCsvPlugin, CsvLine}
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import VwJniModelTest._
import spray.json._
import spray.json.DefaultJsonProtocol._
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by rdeak on 6/2/15.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwJniModelTest {

    // TODO: Test numMissingThreshold = Some(0) with CSV lines: "155,100,red",  "180,225,brown",  ",300,gray"
    // TODO: Test numMissingThreshold = None with CSV lines: "155,100,red",  "180,225,brown",  ",300,gray"
    // TODO: Test namespace with features not in features keyset.
    // TODO: Test when namespaces doesn't cover all features.
    // TODO: Test bad VW arguments throw exception.
    // TODO: Test output types: Byte, Short, Integer, Long, Float, Double, String, JavaByte, JavaShort, JavaInteger, JavaLong, JavaFloat, JavaDouble


    @Test def test1(): Unit = {
        val json =
            """
              |{
              |  "modelType": "VwJNI",
              |  "modelId": { "id": 0, "name": "" },
              |  "numMissingThreshold": 0,
              |  "features": {
              |    "height_mm": { "spec": "${height_cm} * 10", "defVal": [["=UNK", 1]] },
              |    "height_cm": "${height_cm}",
              |    "weight": "ind(${weight} / 10)",
              |    "hair": { "spec": "ind(${hair.color})" }
              |  },
              |  "namespaces": {
              |    "personal_features": [ "height_mm", "weight", "hair" ]
              |  },
              |  "vw": {
              |    "params": [
              |      "--quiet",
              |      "-t"
              |    ]
              |  }
              |}
            """.stripMargin.trim.parseJson

        val mf = ModelFactory.defaultFactory
        val mt = mf.getModel[CsvLine, Float](json, Option(semantics))
        val m = mt.get.asInstanceOf[VwJniModel[CsvLine, Float]]
        val csvs = csvLines(
            "155,100,red",
            "180,225,brown",
            ",300,gray"
        )

        val results = csvs.map(c =>
            (
                m.generateVwInput(c),
                m(c)
            ))

        val res2 = csvs.map(c => m.score(c))
        val a = 1
    }
}

object VwJniModelTest {
    val columns = Seq(
        "height_cm" -> CsvTypes.LongOptionType,
        "weight" -> CsvTypes.IntType,
        "hair.color" -> CsvTypes.StringType
    )

    val csvLines = CsvLines(indices = columns.unzip._1.zipWithIndex.toMap, fs = ",")

    val plugin = CompiledSemanticsCsvPlugin(columns:_*)
    val semantics = CompiledSemantics(TwitterEvalCompiler(), plugin, Seq("scala.math._", "com.eharmony.matching.aloha.feature.BasicFunctions._"))
}
