package com.eharmony.matching.aloha.models.vw.jni

import scala.concurrent.ExecutionContext.Implicits.global

import spray.json._
import spray.json.DefaultJsonProtocol._

import org.junit.Assert._
import org.junit.{Ignore, Test}
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import com.eharmony.matching.aloha.factory.ModelFactory
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CsvLines, CsvTypes, CompiledSemanticsCsvPlugin, CsvLine}
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits._
import VwJniModelTest._

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwJniModelTest {

    @Ignore @Test def testByteOutputType(): Unit = { fail() }
    @Ignore @Test def testShortOutputType(): Unit = { fail() }
    @Ignore @Test def testIntOutputType(): Unit = { fail() }
    @Ignore @Test def testLongOutputType(): Unit = { fail() }
    @Ignore @Test def testFloatOutputType(): Unit = { fail() }
    @Ignore @Test def testDoubleOutputType(): Unit = { fail() }
    @Ignore @Test def testStringOutputType(): Unit = { fail() }
    @Ignore @Test def testJavaByteOutputType(): Unit = { fail() }
    @Ignore @Test def testJavaShortOutputType(): Unit = { fail() }
    @Ignore @Test def testJavaIntegerOutputType(): Unit = { fail() }
    @Ignore @Test def testJavaLongOutputType(): Unit = { fail() }
    @Ignore @Test def testJavaFloatOutputType(): Unit = { fail() }
    @Ignore @Test def testJavaDoubleOutputType(): Unit = { fail() }

    @Ignore @Test def testNoThreshWithMissing(): Unit = { fail() }
    @Ignore @Test def testExceededThresh(): Unit = { fail() }

    @Ignore @Test def testNsWithUndeclaredFeatureNames(): Unit = { fail() }
    @Ignore @Test def testNsDoesntCoverAllFeatureNames(): Unit = { fail() }
    @Ignore @Test def testNssAndDefaultDoesntCoverAllFeatureInd(): Unit = { fail() }
    @Ignore @Test def testDifferentSizedNamesAndFeatures(): Unit = { fail() }

    @Ignore @Test def testBadVwArgsThrowsEx(): Unit = { fail() }


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
