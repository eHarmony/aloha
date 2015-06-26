package com.eharmony.matching.featureSpecExtractor.vw.unlabeled

import scala.concurrent.ExecutionContext.Implicits.global
import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine}
import com.eharmony.matching.featureSpecExtractor.SpecBuilder
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

@RunWith(classOf[BlockJUnit4ClassRunner])
class VwSpecProducerTest {
    @Test def test1() {
        val p = CompiledSemanticsCsvPlugin()
        val sem = CompiledSemantics(TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)), p, Nil)
        val sb = SpecBuilder(sem, List(new VwSpecProducer[CsvLine]))

        val json1 =
            """
              |{
              |  "imports": [],
              |  "features": [ { "name":"x", "spec":"Nil" } ]
              |}
            """.stripMargin.trim

        val xOpt = sb.fromString(json1)
        assertTrue(xOpt.isSuccess)

        val x = xOpt.get
        assertEquals(Seq(0), x.defaultNamespace)
        assertEquals(1, x.featuresFunction.features.size)
        assertEquals("x", x.featuresFunction.features.head._1)
        assertEquals(0, x.featuresFunction.features.head._2.accessors.size)
        assertEquals(0, x.featuresFunction.features.head._2.arity)
        assertTrue(x.namespaces.isEmpty)
        assertEquals(None, x.normalizer)
    }
}
