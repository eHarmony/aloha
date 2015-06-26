package com.eharmony.aloha.dataset.vw

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.dataset.json.SparseSpec
import com.eharmony.aloha.dataset.vw.VwCovariateProducerTest.{X, semantics}
import com.eharmony.aloha.dataset.vw.json.VwJsonLike
import com.eharmony.aloha.dataset.{CompilerFailureMessages, SparseCovariateProducer, SparseFeatureExtractorFunction}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvTypes}
import com.eharmony.aloha.semantics.func.GenAggFunc
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
