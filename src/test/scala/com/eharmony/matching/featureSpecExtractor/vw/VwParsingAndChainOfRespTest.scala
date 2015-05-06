package com.eharmony.matching.featureSpecExtractor.vw

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.{ClassTag, classTag}
import scala.util.{Failure, Success}

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvTypes}
import com.eharmony.matching.featureSpecExtractor.vw.cb.{VwContextualBanditSpec, VwContextualBanditSpecProducer}
import com.eharmony.matching.featureSpecExtractor.vw.labeled.{VwLabelSpec, VwLabelSpecProducer}
import com.eharmony.matching.featureSpecExtractor.vw.unlabeled.{VwSpec, VwSpecProducer}
import com.eharmony.matching.featureSpecExtractor.{Spec, SpecBuilder, SpecProducer}



/**
 * Test that parsing works correctly with three different Vw spec producers in every permuted order.  This implicitly
 * tests many things.  It tests that the specification order of the SpecProducer instances matters in terms of the
 * value and type of the resulting Spec instance.  This test also implicitly tests that
 *
 1   a specification that can be parsed as a [[VwLabelSpec]] must also be able to be parsed as a [[VwSpec]]
 1   a specification that can be parsed as a [[VwContextualBanditSpec]] must also be able to be parsed as a [[VwSpec]]
 *
 * @author R M Deak
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
class VwParsingAndChainOfRespTest {
    import VwParsingAndChainOfRespTest.semantics

    /**
     * Test the chain of responsibility for VW spec producers on a JSON that represents a VwLabelSpec.
     */
    @Test def testVwChainOfRespForVwLabelSpec(): Unit =
        test[VwLabelSpec[CsvLine], VwLabelSpecProducer[CsvLine]]("com/eharmony/matching/featureSpecExtractor/simpleSpec.json")

    /**
     * Test the chain of responsibility for VW spec producers on a JSON that represents a VwContextualBanditSpec.
     */
    @Test def testVwChainOfRespForVwCbSpec(): Unit =
        test[VwContextualBanditSpec[CsvLine], VwContextualBanditSpecProducer[CsvLine]]("com/eharmony/matching/featureSpecExtractor/simpleCbSpec.json")


    def test[S <: Spec[CsvLine]: ClassTag, P <: SpecProducer[CsvLine, S]: ClassTag](jsonResourceLoc: String) {
        val producers = List(
            new VwContextualBanditSpecProducer[CsvLine],
            new VwLabelSpecProducer[CsvLine],
            new VwSpecProducer[CsvLine]
        )

        producers.permutations foreach { prod =>
            val sb = SpecBuilder(semantics, prod)
            sb.fromResource(jsonResourceLoc) match {
                case Success(s) => verifySpecType[CsvLine, S, P](prod, s)
                case Failure(f) => fail(s"A Spec instance couldn't be produced for $jsonResourceLoc.  Error: $f")
            }
        }
    }

    private[this] def verifySpecType[A, S <: Spec[A]: ClassTag, P <: SpecProducer[A, S]: ClassTag](producers: Seq[SpecProducer[A, VwSpec[A]]], spec: VwSpec[A]) {
        val unlabeled = producers.indexWhere(_.name == classOf[VwSpecProducer[A]].getSimpleName)
        assertTrue(classOf[VwSpecProducer[A]].getSimpleName + " not found in producers", unlabeled != -1)

        val pName = classTag[P].runtimeClass.getSimpleName
        val p = producers.indexWhere(_.name == pName)
        assertTrue(pName + " not found in producers", p != -1)

        // If negative, unlabeled occurs earlier in the list so the the spec should be unlabeled.
        (unlabeled - p).signum match {
            case -1 => assertEquals(
                            s"Error for producers: ${producers.map(_.name).mkString(", ")}",
                            classOf[VwSpec[A]].getCanonicalName,
                            spec.getClass.getCanonicalName)
            case 1 => assertEquals(
                            s"Error for producers: ${producers.map(_.name).mkString(", ")}",
                            classTag[S].runtimeClass.getCanonicalName,
                            spec.getClass.getCanonicalName)
        }
    }
}

object VwParsingAndChainOfRespTest {

    val semantics: CompiledSemantics[CsvLine] = {

        val features = Seq(
            "profile.first_name" -> CsvTypes.StringOptionType,
            "profile.number_of_marriages" -> CsvTypes.IntOptionType,
            "profile.user_id" -> CsvTypes.LongOptionType,
            "profile.matching_address.state" -> CsvTypes.IntOptionType,
            "profile.education" -> CsvTypes.IntOptionType,
            "profile.income" -> CsvTypes.IntOptionType,
            "profile.matching_address.state" -> CsvTypes.IntOptionType,
            "dv.matches" -> CsvTypes.IntOptionType,
            "dv.converted" -> CsvTypes.IntOptionType,
            "dv.match_odds" -> CsvTypes.DoubleOptionType
        )

        val csvPlugin = CompiledSemanticsCsvPlugin(features.toMap)

        CompiledSemantics(
            TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
            csvPlugin,
            Seq.empty)
    }
}
