package com.eharmony.aloha.models.reg

import com.eharmony.aloha.FileLocations
import com.eharmony.aloha.audit.impl.OptionAuditor
import com.eharmony.aloha.audit.impl.tree.RootedTreeAuditor
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.compiled.compiler.TwitterEvalCompiler
import com.eharmony.aloha.semantics.compiled.{CompiledSemantics, CompiledSemanticsPlugin, OptionalAccessorCode}
import com.eharmony.aloha.semantics.func.{GenFunc, GeneratedAccessor, OptionalFunc}
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.immutable

@RunWith(classOf[BlockJUnit4ClassRunner])
class RegressionModelCompiledSemanticsTest {
  import RegressionModelCompiledSemanticsTest.simpleModel

  /** When a required fields is missing, we are at the mercy of the errors.  No attempt to recover these errors as
    * they should indicate a big problem.
    */
  @Test(expected = classOf[NoSuchElementException]) def testMissingRequired() {
    simpleModel(Map("user.profile.education" -> 4))
  }

  @Test def testSuccessWithMissingData() {
    val y = simpleModel(Map("user.profile.income" -> 1))
    assertEquals(0.023, y.value.get, 1.0e-6)
    assertEquals(1, y.missingVarNames.size)
    val missing = y.missingVarNames.mkString(",")
    assertEquals("user.profile.education", missing)
  }

  @Test def testSuccessNoMissingData() {
    val y = simpleModel(Map("user.profile.education" -> 4, "user.profile.income" -> 1))
    assertEquals(0.1234, y.value.get, 1.0e-6)
  }

  @Test def testParsing1() {

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
      "com.eharmony.aloha.feature.BasicFunctions._"
    )

    val plugin = new CompiledSemanticsPlugin[Map[String, Long]] {
      def refInfoA: RefInfo[Map[String, Long]] = RefInfo[Map[String, Long]]
      def accessorFunctionCode(spec: String) = Right(OptionalAccessorCode(Seq(s"""(_:$inputTypeString).get("$spec")""")))
    }

    // This will speed up repeated tests.
    val compiler = TwitterEvalCompiler(classCacheDir = Option(FileLocations.testGeneratedClasses))

    import concurrent.ExecutionContext.Implicits.global
    val semantics = CompiledSemantics(compiler, plugin, imports)

    val f = ModelFactory.defaultFactory(semantics, OptionAuditor[Double]())

    val m = f.fromString(json).get

    val x = Map[String, Long](
      "user.profile.education" -> 4,
      "user.profile.income" -> 5,
      "cand.profile.income" -> 2,
      "pair.distance" -> 8
    )

    m(x)
  }
}

object RegressionModelCompiledSemanticsTest {
  private def ind(a: Any) = Seq(((a.toString), 1.0))
  private def log2(a: Double) = math.log(a) / math.log(2)
  private def sos2(a: Double) = Seq((s"=${a.toInt}", a.toInt + 1 - a), (s"=${a.toInt+1}", a - a.toInt))

  private lazy val simpleModel = {
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
    RegressionModel(mId, features.keys.toIndexedSeq, features.values.toIndexedSeq, w, identity, None, None, RootedTreeAuditor.noUpperBound[Double]())
  }

}
