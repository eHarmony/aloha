package com.eharmony.matching.featureSpecExtractor.vw

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvTypes}
import com.eharmony.matching.aloha.semantics.func.GenAggFunc
import com.eharmony.matching.featureSpecExtractor.json.SparseSpec
import com.eharmony.matching.featureSpecExtractor.vw.VwCovariateProducerTest.{X, semantics}
import com.eharmony.matching.featureSpecExtractor.vw.json.VwJsonLike
import com.eharmony.matching.featureSpecExtractor.{CompilerFailureMessages, SparseCovariateProducer, SparseFeatureExtractorFunction}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

/**
 * Test that VwCovariateProducer.getVwData Works correctly
 * @author R M Deak
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwCovariateProducerTest {
    @Test def testGetVwDataWith1Function() {
        val j = new VwJsonLike {
            val namespaces = None
            val normalizeFeatures = None
            val features = Vector(SparseSpec("i_plus_d", """List(("", ${i} + ${d}))"""))
            val imports = Nil
        }

        val (covariates, default, nss, normalizer) = X.getVwData(semantics, j)

        covariates match {
            case Success(SparseFeatureExtractorFunction(IndexedSeq(("i_plus_d", f)))) =>
                assertTrue("Wrong covariate function", f.isInstanceOf[GenAggFunc[CsvLine, Iterable[(String, Double)]]])
            case _ => fail("Wrong covariates.")
        }

        assertEquals(1, default.size)
        assertEquals(0, nss.size)
        assertEquals(None, normalizer)
    }

    @Test def testGetVwDataEverythingMissing() {

        val j = new VwJsonLike {
            val namespaces = None
            val normalizeFeatures = None
            val features = Vector.empty
            val imports = Nil
        }

        val (covariates, default, nss, normalizer) = X.getVwData(semantics, j)

        covariates match {
            case Success(SparseFeatureExtractorFunction(IndexedSeq())) =>
            case _ => fail("Wrong covariates.")
        }

        assertEquals(0, default.size)
        assertEquals(0, nss.size)
        assertEquals(None, normalizer)
    }
}

private object VwCovariateProducerTest {
    object X extends VwCovariateProducer[CsvLine] with SparseCovariateProducer with CompilerFailureMessages {
        // To expose for testing.
        override def getVwData(semantics: CompiledSemantics[CsvLine], json: VwJsonLike) =
            super.getVwData(semantics, json)
    }

    lazy val semantics = {
        val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))
        val plugin = CompiledSemanticsCsvPlugin(
            "i" -> CsvTypes.IntType,
            "d" -> CsvTypes.DoubleType
        )
        CompiledSemantics[CsvLine](compiler, plugin, Nil)
    }
}
