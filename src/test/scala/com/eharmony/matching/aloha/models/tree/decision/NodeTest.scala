package com.eharmony.matching.aloha.models.tree.decision

import scala.collection.JavaConversions.asScalaBuffer

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert._

import scala.collection.immutable
import com.eharmony.matching.aloha.semantics.func.{GeneratedAccessor, GenFunc, GenAggFunc}
import com.eharmony.matching.aloha.util.rand.HashedCategoricalDistribution
import com.eharmony.matching.aloha.semantics.Semantics
import com.eharmony.matching.aloha.reflect.RefInfo
import com.eharmony.matching.aloha.factory.ModelFactory

import spray.json.DefaultJsonProtocol.IntJsonFormat
import com.eharmony.matching.aloha.score.conversions.ScoreConverter.Implicits.IntScoreConverter
import com.eharmony.matching.aloha.score.conversions.rich.RichScore

@RunWith(classOf[BlockJUnit4ClassRunner])
class NodeTest {
    import NodeTest._

    @Test def testNodeWithNoMissingFeatures() = testRandomDistGoingToNthIndex(FeatureName, Option(-1))

    @Test def testNodeWithOneMissingFeature() = testRandomDistGoingToNthIndex(FeatureName, None)

    @Test def fullTest() {
        val model = modelFromJson(FeatureName)
        val s = model.score(Map.empty)
        assertTrue("should have score in score", s.hasScore)
        assertTrue("should have error in score", s.hasError)
        assertEquals("missing", Seq(FeatureName), s.getError.getMissingFeatures.getNamesList.toSeq)
        assertEquals("score", 0, s.relaxed.asInt.get)
    }

    private[this] def modelFromJson(featureName: String) = {
        val json =
            s"""
              |{
              |  "modelType": "DecisionTree",
              |  "modelId": { "id": 0, "name": "" },
              |  "returnBest": true,
              |  "missingDataOk": true,
              |  "nodes": [
              |    {
              |      "id": 0,
              |      "value": 0,
              |      "selector": {
              |        "selectorType": "random",
              |        "children": [1],
              |        "features": ["$featureName"],
              |        "probabilities": [0, 1],
              |        "missingOk": true }
              |    },
              |    { "id": 1, "value": 1 }
              |  ]
              |}
            """.stripMargin

        val m = factory.fromString(json).get
        m
    }

    private[this] val semantics = new Semantics[Map[String, Double]] {
        def refInfoA = RefInfo[Map[String, Double]]
        def close() {}
        def accessorFunctionNames: Seq[String] = Nil
        def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Map[String, Double], B]] = {
            val cs = codeSpec.trim
            val ga = GeneratedAccessor(cs, (_: Map[String, Double]).get(cs), Option("""(_: Map[String, Double]).get(cs)"""))
            val f = GenFunc.f1(ga)("${"+cs+"} >= 0", _ map {_ >= 0})
            val g = Right(f.asInstanceOf[GenAggFunc[Map[String, Double], B]])
            g
        }
    }

    private[this] val factory = ModelFactory.defaultFactory.toTypedFactory[Map[String, Double], Int](semantics)

    private[this] def testRandomDistGoingToNthIndex(featureName: String, input: Option[Int]) {
        val acc = GeneratedAccessor(featureName, identity[Option[Int]])
        val gaf = GenFunc.f1(acc)("", _.map(_ < 1))

        val features = Seq(gaf)
        val selectInd1 = RandomNodeSelector(features, HashedCategoricalDistribution(0, 1), true)
        val node = InteriorNode(1, immutable.IndexedSeq.empty, selectInd1)

        val current = node.getNode(input)
        assertTrue(current.isLeft)

        val intNodeRes = current.left.get
        assertEquals("errors", Seq("childSelector returned i = 1. Size = 0"), intNodeRes.errors)

        val missing = if (input.isEmpty) List(featureName) else Nil
        assertEquals("missing", missing, intNodeRes.missing)
        assertEquals("node", node, intNodeRes.node)
    }
}

private object NodeTest {
    val FeatureName = "120oubnqgwe"
}
