package com.eharmony.matching.aloha.models.vw.jni

import scala.concurrent.ExecutionContext.Implicits.global

import org.junit.Assert._
import org.junit.{Ignore, Test}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import spray.json.DefaultJsonProtocol._
import spray.json._

import com.eharmony.matching.aloha.factory.ModelFactory
import com.eharmony.matching.aloha.models.vw.jni.VwJniModelTest._
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}


@Ignore @RunWith(classOf[BlockJUnit4ClassRunner])
class VwJniModelTest {

    @Test def testByteOutputType(): Unit = { fail() }
    @Test def testShortOutputType(): Unit = { fail() }
    @Test def testIntOutputType(): Unit = { fail() }
    @Test def testLongOutputType(): Unit = { fail() }
    @Test def testFloatOutputType(): Unit = { fail() }
    @Test def testDoubleOutputType(): Unit = { fail() }
    @Test def testStringOutputType(): Unit = { fail() }
    @Test def testJavaByteOutputType(): Unit = { fail() }
    @Test def testJavaShortOutputType(): Unit = { fail() }
    @Test def testJavaIntegerOutputType(): Unit = { fail() }
    @Test def testJavaLongOutputType(): Unit = { fail() }
    @Test def testJavaFloatOutputType(): Unit = { fail() }
    @Test def testJavaDoubleOutputType(): Unit = { fail() }

    @Test def testNoThreshWithMissing(): Unit = { fail() }
    @Test def testExceededThresh(): Unit = { fail() }

    @Test def testNsWithUndeclaredFeatureNames(): Unit = { fail() }
    @Test def testNsDoesntCoverAllFeatureNames(): Unit = { fail() }
    @Test def testNssAndDefaultDoesntCoverAllFeatureInd(): Unit = { fail() }
    @Test def testDifferentSizedNamesAndFeatures(): Unit = { fail() }

    @Test def testBadVwArgsThrowsEx(): Unit = { fail() }


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

    val typeTestJson =
        """
          |{
          |  "modelType": "VwJNI",
          |  "modelId": { "id": 0, "name": "" },
          |  "numMissingThreshold": 0,
          |  "features": {
          |    "weight": "ind(${weight} / 10)"
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

    val csvLines = CsvLines(indices = columns.unzip._1.zipWithIndex.toMap, fs = ",")
    val plugin = CompiledSemanticsCsvPlugin(columns:_*)
    val semantics = CompiledSemantics(TwitterEvalCompiler(), plugin, Seq("scala.math._", "com.eharmony.matching.aloha.feature.BasicFunctions._"))
}
