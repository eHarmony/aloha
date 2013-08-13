package com.eharmony.matching.aloha.models.tree.decision

import collection.JavaConversions.asScalaBuffer

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

import spray.json.JsValue

import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.semantics.func.{GenFunc, GeneratedAccessor, GenAggFunc}
import com.eharmony.matching.aloha.util.rand.HashedCategoricalDistribution

@RunWith(classOf[BlockJUnit4ClassRunner])
class RandomNodeSelectorTest {

    /** Create a decision tree that has a root value and two leaf nodes with values 0 and 1 and transition
      * probabilities ''1 - p'' and ''p'', respectively.  If the transition probabilities are working correctly, we
      * expect the values returned by the decision tree model to be Bernoulli distributed.
      *
      * {{{
      * //          1 - p
      * // -2^31^ --------> 0
      * //        \
      * //         \    p
      * //          ------> 1
      * }}}
      *
      * This test checks that the empirical mean and variance of the values returned by the models appoximately matches
      * those of the Bernoulli distribution.
      */
    @Test def testTwoSplitCorrectnessIsApproximatelyBernoulli() {
        val n = 10000

        // Make sure to test the 0 and 1 probabilities, inclusive.
        (0 to 10).map(i => {
            val p = i / 10.0

            val tree = model(getTreeJson(true, true, p))

            // Compute the number of successes from the decision tree given a success probability of p.
            // This shows that the
            // Should be 0L to avoid possible underflow that could miraculously, yet fallaciously cause
            val s = (1 to n).foldLeft(0L)((s, x) => s + tree(Map("F" -> x.toDouble)).getOrElse(Int.MinValue))

            // Assert that the empirical mean is close to the mean of the Bernoulli distribution.  Note that if
            // the root value (-2^31) is returned even once by the model, this assertion will fail.  Therefore, this
            // should appear before the variance assertion.
            val pAct = s / n.toDouble
            assertEquals(p, pAct, 0.01)

            // Assert that the empirical variance is close to the variance of the Bernoulli distribution.
            val bernoulliVariance = p * (1 - p)
            val varAct = 1.0 / (n - 1) * ((n - s)*pAct*pAct + s * (1 - pAct)*(1 - pAct))
            assertEquals(bernoulliVariance, varAct, 0.005)
        })
    }

    @Test def test_returnBest_false__missingOK_false() {
        val s = model(getTreeJson(false, false, 0.5)).score(Map.empty)
        assertFalse("Score should NOT have a value.", s.hasScore)
        assertTrue("Score should have an error.", s.hasError)
        assertEquals("Incorrect list of missing features.", Seq("F"), s.getError.getMissingFeatures.getNamesList.toSeq)
    }

    @Test def test_returnBest_true__missingOK_false() {
        val s = model(getTreeJson(true, false, 0.5)).score(Map.empty)
        assertTrue("Score should have an error.", s.hasError)
        assertEquals("Incorrect list of missing features.", Seq("F"), s.getError.getMissingFeatures.getNamesList.toSeq)
        assertTrue("Score should have a value.", s.hasScore)
        import com.eharmony.matching.aloha.score.conversions.rich.RichScore
        assertEquals("Unexpected score returned by the model. Should be the value at the root.", Some(Int.MinValue), s.relaxed.asInt)
    }

    @Test def test_missingOK_true() {
        // This is part of the test because we want to make sure that when we get back the sequence of hashes,
        // we get the right index.
        val d = HashedCategoricalDistribution(0.5, 0.5)
        val x = Vector(None, Some(1))
        val di = d(x)
        assertEquals("The second branch (index 1) should be selected given the U(0, 1) distribution", 1, di)


        import com.eharmony.matching.aloha.score.conversions.rich.RichScore
        Seq(true, false).foreach { b => {
            val s = model(getTreeJson(b, true, 0.5)).score(Map.empty)
            assertFalse("Score should NOT have an error.", s.hasError)
            assertTrue("Score should have a value.", s.hasScore)
            assertEquals("Unexpected score returned by the model.", Some(1), s.relaxed.asInt)
        }}
    }

    private[this] def model(json: JsValue) = {
        import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
        import spray.json.DefaultJsonProtocol.IntJsonFormat
        val reader = BasicDecisionTree.parser.modelJsonReader[Map[String, Double], Int](randomTestSemantics)
        val m = reader.read(json)
        m
    }

    /** Creates GenAggFunc[ Map[String, Double], Option[Any] ] cast to GenAggFunc[ Map[String, Double], Option[B] ].
      * This is to be used for testing of random branching in decision trees.
      */
    object randomTestSemantics extends Semantics[Map[String, Double]] {
        // Regexs from: scala.util.parsing.combinator.JavaTokenParsers
        private[this] val wholeNumber = """(-?\d+)""".r.anchored
        private[this] val decimalNumber = """(\d+(\.\d*)?|\d*\.\d+)""".r.anchored
        private[this] def intValue(s: String) = GenFunc.f0(s, (_: Any) => Option(s.toInt))
        private[this] def doubleValue(s: String) = GenFunc.f0(s, (_: Any) => Option(s.toDouble))

        def refInfoA = RefInfo[Map[String, Double]]
        def accessorFunctionNames = Nil
        def close() {}

        /** Creates GenAggFunc[ Map[String, Double], Option[Any] ] cast to GenAggFunc[ Map[String, Double], Option[B] ].
          * @param codeSpec specification for a function to be produced by this semantics.
          * @param default a default value in the case that the function would produce an optional type.
          * @tparam B The return type of the function.
          * @return
          */
        def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Map[String, Double], B]] = {
            val func: GenAggFunc[Map[String, Double], Option[Any]] = codeSpec.trim match {
                case wholeNumber(z) => intValue(z)
                case decimalNumber(r, _) => doubleValue(r)
                case cs =>
                    val ga = GeneratedAccessor(cs, (_: Map[String, Double]).get(cs), Option("""(_: Map[String, Double]).get(cs)"""))
                    val f = GenFunc.f1(ga)("${\"+cs+\"}", identity)
                    f
            }

            // Cast to the proper type.
            Right(func.asInstanceOf[GenAggFunc[Map[String, Double], B]])
        }
    }

    private[this] def getTreeJson(returnBest: Boolean, missingDataOk: Boolean, pr: Double): JsValue = {
        val pr0 = 1 - pr
        import spray.json.pimpString
        val json =
            s"""
               |{
               |  "modelType": "DecisionTree",
               |  "modelId": {"id": 0, "name": ""},
               |  "returnBest": $returnBest,
               |  "missingDataOk": $missingDataOk,
               |  "nodes": [
               |    {
               |      "id": -2147483648,
               |      "value": -2147483648,
               |      "selector": { "selectorType": "random", "children": [0, 1], "features": ["F", "1"], "probabilities": [$pr0, $pr] }
               |    },
               |    { "id": 0, "value": 0 },
               |    { "id": 1, "value": 1 }
               |  ]
               |}
            """.stripMargin.trim.asJson
        json
    }

    //    @Test def test1() {
    //        val n = 1000
    //        val k = 10
    //
    //        val max = Int.MaxValue.toFloat
    //        val toF = (n: Int) => math.abs(n) / max
    //
    //        val f = (n: Int) => Array(Option(n))
    //        val mh3 = f andThen { a => MurmurHash3.orderedHash(a, MurmurHash3.arraySeed) } andThen { toF }
    //        val mh = f andThen { MurmurHash arrayHash _ } andThen { toF }
    //
    //        val mh3s = it(n).map(mh3).toArray.sorted
    //        val mhs = it(n).map(mh).toArray.sorted
    //        println(mh3s(n/2))
    //        println(variance(mh3s))
    //
    ////        val mh3Dist = dist(it(n), mh3, n, k)
    ////        val mhDist = dist(it(n), mh, n, k)
    ////        val uniformK = Array.fill(k)(1.0/k)
    ////
    ////        val mhNonRandomUniform = JensenShannonDivergence.withinTolerance(mhDist, uniformK, n)
    ////        val mh3NonRandomUniform = JensenShannonDivergence.withinTolerance(mh3Dist, uniformK, n)
    ////
    ////        val mhNonRandomJsd = JensenShannonDivergence.get(mhDist, uniformK)
    ////        val mh3NonRandomJsd = JensenShannonDivergence.get(mh3Dist, uniformK)
    ////
    ////        val mhRandDist = dist(randIt(n), mh, n, k)
    ////        val mh3RandDist = dist(randIt(n), mh3, n, k)
    ////
    ////        val mhRandomUniform = JensenShannonDivergence.withinTolerance(mhRandDist, uniformK, n)
    ////        val mh3RandomUniform = JensenShannonDivergence.withinTolerance(mh3RandDist, uniformK, n)
    ////
    ////        val mhRandomJsd = JensenShannonDivergence.get(mhRandDist, uniformK)
    ////        val mh3RandomJsd = JensenShannonDivergence.get(mh3RandDist, uniformK)
    //
    //        val a = 1
    //    }
    //
    //    def variance[Double](data: Traversable[Float]) = {
    //        val mean = data.sum / data.size
    //        data.reduceLeft((s, x) => s + (x - mean)*(x - mean)) / (data.size - 1)
    //    }
    //
    //    def dist[A](it: Iterator[A], f: A => Float, n: Int, k: Int) =
    //        it.foldLeft(new Array[Double](k)){ case(a, x) => {a((f(x) * k).toInt) += 1; a} } map { _ / n }
    //
    //    def it(n: Int) = Iterator.range(0, n)
    //
    //    def randIt(n: Int) = {
    //        val r = new Random(0)
    //        Iterator.fill(n)(r.nextInt())
    //    }
}
