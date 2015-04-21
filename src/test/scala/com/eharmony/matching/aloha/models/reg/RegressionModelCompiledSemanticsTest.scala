package com.eharmony.matching.aloha.models.reg

import collection.JavaConversions.asScalaBuffer

import org.junit.Test
import org.junit.Assert._
import org.junit.runner.RunWith

import com.eharmony.matching.aloha.id.ModelId
import com.eharmony.matching.aloha.factory.ModelFactory
import com.eharmony.matching.aloha.semantics.func.{OptionalFunc, GeneratedAccessor, GenFunc}
import com.eharmony.matching.aloha.score.conversions.rich.RichScore
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.DoubleScoreConverter
import com.eharmony.matching.aloha.semantics.compiled.{CompiledSemantics, OptionalAccessorCode, CompiledSemanticsPlugin}
import com.eharmony.matching.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.matching.aloha.FileLocations
import com.eharmony.matching.aloha.reflect.RefInfo
import org.junit.runners.BlockJUnit4ClassRunner
import com.eharmony.matching.aloha.models.Model
import scala.collection.immutable

@RunWith(classOf[BlockJUnit4ClassRunner])
class RegressionModelTestCompiledSemanticsTest {
    import RegressionModelTestCompiledSemanticsTest.simpleModel

    /** When a required fields is missing, we are at the mercy of the errors.  No attempt to recover these errors as
      * they should indicate a big problem.
      */
    @Test(expected = classOf[NoSuchElementException]) def testMissingRequired() {
        simpleModel.score(Map("user.profile.education" -> 4))
    }

    @Test def testSuccessWithMissingData() {
        val y = simpleModel.score(Map("user.profile.income" -> 1))
        assertEquals(0.023, y.relaxed.asDouble.get, 1.0e-6)
        assertEquals(1, y.getError.getMissingFeatures.getNamesCount)
        val missing = asScalaBuffer(y.getError.getMissingFeatures.getNamesList).mkString(",")
        assertEquals("user.profile.education", missing)
    }

    @Test def testSuccessNoMissingData() {
        val y = simpleModel.score(Map("user.profile.education" -> 4, "user.profile.income" -> 1))
        assertEquals(0.1234, y.relaxed.asDouble.get, 1.0e-6)
    }

    @Test def testParsing1() {
        import spray.json.DefaultJsonProtocol.DoubleJsonFormat

        val json =
            """
              |{
              |    "modelType": "Regression",
              |    "modelId": {"id": 10, "name": "asdf"},
              |    "features": {
              |        "ueducation_cincome": "${user.profile.education} * ${cand.profile.income}",
              |        "user_education": "ind(${user.profile.education})",
              |        "user_income": {
              |          "spec": "ind(${user.profile.income})",
              |          "defVal": [["=income_error", 1]]
              |        },
              |        "distance": "sos2(log2(${pair.distance}), 0, 10, 1)"
              |    },
              |    "weights": {
              |        "user_education=4": 0.5,
              |        "user_income=5": 0.25,
              |        "distance=3": 0.4,
              |        "distance=4": 0.3
              |    },
              |    "higherOrderFeatures": [
              |        {"features": { "user_education": ["user_education=4"], "user_income": ["user_income=5"] }, "wt": 0.25 }
              |    ],
              |    "spline": {
              |        "min": 0,
              |        "max": 2,
              |        "knots": [0, 2]
              |    }
              |}
            """.stripMargin

        val imports = Seq(
            "scala.math._",
            "com.eharmony.matching.aloha.feature.BasicFunctions._"
        )

        val plugin = new CompiledSemanticsPlugin[Map[String, Long]] {
            def refInfoA = RefInfo[Map[String, Long]]
            def accessorFunctionCode(spec: String) = Right(OptionalAccessorCode(Seq(s"""(_:$inputTypeString).get("$spec")""")))
        }

        // This will speed up repeated tests.
        val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))

        import concurrent.ExecutionContext.Implicits.global
        val semantics = CompiledSemantics(compiler, plugin, imports)

        val f = ModelFactory(RegressionModel.parser).toTypedFactory[Map[String, Long], Double](semantics)

        val m: Model[Map[String, Long], Double] = f.fromString(json).get

        val x = Map[String, Long](
            "user.profile.education" -> 4,
            "user.profile.income" -> 5,
            "cand.profile.income" -> 2,
            "pair.distance" -> 8
        )

        m.score(x)
    }
}

object RegressionModelTestCompiledSemanticsTest {
    def ind(a: Any) = Seq(((a.toString), 1.0))
    def log2(a: Double) = math.log(a) / math.log(2)
    def sos2(a: Double) = Seq((s"=${a.toInt}", a.toInt + 1 - a), (s"=${a.toInt+1}", a - a.toInt))

    private lazy val simpleModel: RegressionModel[Map[String, Long], Double] = {
        val mId = ModelId.empty

        //        val features = IndexedSeq(
        //
        //            OptionalFunc(
        //                GenFunc.f1(
        //                    GeneratedAccessor(
        //                        "user.profile.education",
        //                        (_:Map[String, Long]).get("user.profile.education")))(
        //                    "ind(${user.profile.education}).",
        //                    (o1) => for (e <- o1) yield Iterable(("education=" + e, 1.0))
        //                ),
        //                Nil
        //            ),
        //
        //            GenFunc.f1(
        //                GeneratedAccessor(
        //                    "user.profile.income",
        //                    (_:Map[String, Long]).apply("user.profile.income")))(
        //                "ind(${user.profile.income}).",
        //                (income) => Iterable(("income=" + income, 1.0))
        //            )
        //        )

        val features = immutable.ListMap(

            "education" -> OptionalFunc(
                GenFunc.f1(
                    GeneratedAccessor(
                        "user.profile.education",
                        (_:Map[String, Long]).get("user.profile.education")))(
                    "ind(${user.profile.education}).",
                    (o1) => for (e <- o1) yield Iterable((s"=$e", 1.0))
                ),
                Nil
            ),

            "income" -> GenFunc.f1(
                GeneratedAccessor(
                    "user.profile.income",
                    (_:Map[String, Long]).apply("user.profile.income")))(
                "ind(${user.profile.income}).",
                (income) => Iterable((s"=$income", 1.0))
            )
        )

        val w = (PolynomialEvaluator.builder ++= Map(
            Map("education=1" -> 0) -> 0.0001,
            Map("education=2" -> 0) -> 0.0002,
            Map("education=3" -> 0) -> 0.0003,
            Map("education=4" -> 0) -> 0.0004,
            Map("income=1" -> 1) -> 0.023,
            Map("income=2" -> 1) -> 0.034,
            Map("income=3" -> 1) -> 0.045,
            Map("income=4" -> 1) -> 0.056,
            Map("education=4" -> 0, "income=1" -> 1) -> 0.1
        )).result()

        // RegressionModel(mid, features, featureFunctions, beta, invLink, spline, numMissing)
        RegressionModel(mId, features.keys.toIndexedSeq, features.values.toIndexedSeq, w, identity, None, None)
    }

}
