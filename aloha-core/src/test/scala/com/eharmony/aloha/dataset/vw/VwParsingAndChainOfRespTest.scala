package com.eharmony.aloha.dataset.vw

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.dataset.vw.cb.VwContextualBanditRowCreator
import com.eharmony.aloha.dataset.vw.labeled.VwLabelRowCreator
import com.eharmony.aloha.dataset.vw.unlabeled.VwRowCreator
import com.eharmony.aloha.dataset.{CharSeqRowCreator, RowCreatorBuilder, RowCreatorProducer}
import com.eharmony.aloha.semantics.compiled.CompiledSemantics
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.plugin.csv.{CompiledSemanticsCsvPlugin, CsvLine, CsvLines, CsvTypes}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.{ClassTag, classTag}
import scala.util.{Failure, Success}



/**
 * Test that parsing works correctly with three different Vw spec producers in every permuted order.  This implicitly
 * tests many things.  It tests that the specification order of the SpecProducer instances matters in terms of the
 * value and type of the resulting Spec instance.  This test also implicitly tests that
 *
 1   a specification that can be parsed as a [[VwLabelRowCreator]] must also be able to be parsed as a [[VwRowCreator]]
 1   a specification that can be parsed as a [[VwContextualBanditRowCreator]] must also be able to be parsed as a [[VwRowCreator]]
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
        test[VwLabelRowCreator[CsvLine], VwLabelRowCreator.Producer[CsvLine]]("com/eharmony/aloha/dataset/simpleSpec.json")

    /**
     * Test the chain of responsibility for VW spec producers on a JSON that represents a VwContextualBanditSpec.
     */
    @Test def testVwChainOfRespForVwCbSpec(): Unit =
        test[VwContextualBanditRowCreator[CsvLine], VwContextualBanditRowCreator.Producer[CsvLine]]("com/eharmony/aloha/dataset/simpleCbSpec.json")


    def test[S <: CharSeqRowCreator[CsvLine]: ClassTag, P <: RowCreatorProducer[CsvLine, CharSequence, S]: ClassTag](jsonResourceLoc: String) {
        val producers = List(
            new VwContextualBanditRowCreator.Producer[CsvLine],
            new VwLabelRowCreator.Producer[CsvLine],
            new VwRowCreator.Producer[CsvLine]
        )

        producers.permutations foreach { prod =>
            val sb = RowCreatorBuilder(semantics, prod)
            sb.fromResource(jsonResourceLoc) match {
                case Success(s) => verifySpecType[CsvLine, S, P](prod, s)
                case Failure(f) => fail(s"A Spec instance couldn't be produced for $jsonResourceLoc.  Error: $f")
            }
        }
    }

    private[this] def verifySpecType[A, S <: CharSeqRowCreator[A]: ClassTag, P <: RowCreatorProducer[A, CharSequence, S]: ClassTag](producers: Seq[RowCreatorProducer[A, CharSequence, VwRowCreator[A]]], spec: VwRowCreator[A]) {
        val unlabeled = producers.indexWhere(_.name == classOf[VwRowCreator[A]].getSimpleName + "." + classOf[VwRowCreator.Producer[A]].getSimpleName)
        assertTrue(classOf[VwRowCreator.Producer[A]].getSimpleName + " not found in producers", unlabeled != -1)

        val pName = classTag[P].runtimeClass.getEnclosingClass.getSimpleName + ".Producer"
        val p = producers.indexWhere(_.name == pName)
        assertTrue(pName + " not found in producers", p != -1)

        // If negative, unlabeled occurs earlier in the list so the the spec should be unlabeled.
        (unlabeled - p).signum match {
            case -1 => assertEquals(
                            s"Error for producers: ${producers.map(_.name).mkString(", ")}",
                            classOf[VwRowCreator[A]].getCanonicalName,
                            spec.getClass.getCanonicalName)
            case 1 => assertEquals(
                            s"Error for producers: ${producers.map(_.name).mkString(", ")}",
                            classTag[S].runtimeClass.getCanonicalName,
                            spec.getClass.getCanonicalName)
            case 0 =>
        }
    }
}

object VwParsingAndChainOfRespTest {

    private[this] val features = Seq(
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

    val semantics: CompiledSemantics[CsvLine] = {
        val csvPlugin = CompiledSemanticsCsvPlugin(features.toMap)

        CompiledSemantics(
            TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses)),
            csvPlugin,
            Seq.empty)
    }

    val csvLines = CsvLines(indices = features.unzip._1.zipWithIndex.toMap, fs = ",")
}
