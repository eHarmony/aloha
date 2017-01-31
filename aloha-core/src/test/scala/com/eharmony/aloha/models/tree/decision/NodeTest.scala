package com.eharmony.aloha.models.tree.decision

import com.eharmony.aloha.audit.impl.TreeAuditor
import com.eharmony.aloha.factory.NewModelFactory
import com.eharmony.aloha.reflect.RefInfo
import com.eharmony.aloha.semantics.Semantics
import com.eharmony.aloha.semantics.func.{GenAggFunc, GenFunc, GeneratedAccessor}
import com.eharmony.aloha.util.rand.HashedCategoricalDistribution
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

import scala.collection.immutable

/** Check that ''Node''s are behaving correctly especially when the index returned by the node's node selector is
  * greater than or equal to the node's number of children.
  *
  * The test scenario is to construct a two node decision tree and make the root node's node selector return an
  * index 1.  This makes the index equal the number children.  This forces the node to deal with the case when we
  * can't successfully select a child.
  */
@RunWith(classOf[BlockJUnit4ClassRunner])
class NodeTest {
  import NodeTest._

  // These tests are at the node level.

  @Test def testNodeWithNoMissingFeatures() = testRandomDistGoingToNthIndex(FeatureName, Option(-1))

  @Test def testNodeWithOneMissingFeature() = testRandomDistGoingToNthIndex(FeatureName, None)


  // The following are at the model level, not just the tree level.

  @Test def fullTest_t_t_missing() {
    val model = modelFromJson(FeatureName, returnBest = true, missingDataOk = true)
    val s = model(Map.empty)

    assertTrue("Should have score in score: ", s.value.isDefined)
    assertEquals("wrong score: ", Option(0), s.value)

    assertTrue("Should have error in score: ", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
    assertTrue("Shouldn't have any error messages", s.errorMsgs.isEmpty)
    assertEquals("wrong missing features: ", Set(FeatureName), s.missingVarNames)
  }

  @Test def fullTest_t_t_notMissing() {
    val model = modelFromJson(FeatureName, returnBest = true, missingDataOk = true)
    val s = model(Features)

    assertTrue("Should have score in score: ", s.value.isDefined)
    assertEquals("wrong score: ", 0, s.value.get)
    assertFalse("Shouldn't have error in score: ", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
  }

  @Test def fullTest_t_f_missing() {
    val model = modelFromJson(FeatureName, returnBest = true, missingDataOk = false)
    val s = model(Map.empty)

    assertTrue("Should have score in score: ", s.value.isDefined)
    assertEquals("wrong score: ", 0, s.value.get)

    assertTrue("Should have error in score: ", s.missingVarNames.nonEmpty)
    assertTrue("Shouldn't have any error messages", s.errorMsgs.isEmpty)
    assertEquals("wrong missing features: ", Set(FeatureName), s.missingVarNames)
  }

  @Test def fullTest_t_f_notMissing() {
    val model = modelFromJson(FeatureName, returnBest = true, missingDataOk = false)
    val s = model(Features)

    assertTrue("Should have score in score: ", s.value.isDefined)
    assertEquals("wrong score: ", 0, s.value.get)

    assertFalse("Shouldn't have error in score: ", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
  }

  @Test def fullTest_f_t_missing() {
    val model = modelFromJson(FeatureName, returnBest = false, missingDataOk = true)
    val s = model(Map.empty)
    assertTrue("Shouldn't have score in score: ", s.value.isEmpty)

    assertTrue("Should have error in score: ", s.missingVarNames.nonEmpty || s.errorMsgs.nonEmpty)
    assertEquals("Wrong error messages", errMsgs(FeatureName, missingOk = true), s.errorMsgs)
    assertEquals("wrong missing features: ", Set(FeatureName), s.missingVarNames)
  }

  @Test def fullTest_f_t_notMissing() {
    val model = modelFromJson(FeatureName, returnBest = false, missingDataOk = true)
    val s = model(Features)
    assertTrue("Shouldn't have score in score: ", s.value.isEmpty)
    assertTrue("Should have error in score: ", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
    assertEquals("Wrong error messages", errMsgs(FeatureName, missingOk = true), s.errorMsgs)
    assertTrue("Shouldn't have missing features: ", s.missingVarNames.isEmpty)
  }

  @Test def fullTest_f_f_missing() {
    val model = modelFromJson(FeatureName, returnBest = false, missingDataOk = false)
    val s = model(Map.empty)
    assertTrue("Shouldn't have score in score: ", s.value.isEmpty)
    assertTrue("Should have error in score: ", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)

    val err = "Couldn't randomly branch due to missing features: "+FeatureName+" in RandomNodeSelector(Vector(GenAggFunc((${"+FeatureName+"}) => ${"+FeatureName+"} >= 0)),<function1>,false)"
    assertEquals("Wrong error messages", Seq(err), s.errorMsgs)
    assertEquals("wrong missing features: ", Set(FeatureName), s.missingVarNames)
  }

  @Test def fullTest_f_f_notMissing() {
    val model = modelFromJson(FeatureName, returnBest = false, missingDataOk = false)
    val s = model(Features)

    assertTrue("Shouldn't have score in score: ", s.value.isEmpty)
    assertTrue("Should have error in score: ", s.errorMsgs.nonEmpty || s.missingVarNames.nonEmpty)
    assertEquals("Wrong error messages", errMsgs(FeatureName, missingOk = false), s.errorMsgs)
    assertTrue("Shouldn't have missing features: ", s.missingVarNames.isEmpty)
  }

  /** Test a length 2 chain (tree w/ constant branching factor of 1) at the node level vs. the tree level.  There
    * is a chain with a root containing a value of 0 that has one child with a value of 1.  The root's node selector
    * is a random selector that always selects the (zero-indexed) "1st child".  Since there is no child at index 1,
    * we are testing the node's behavior when the node's node selector returns an index outside the range of indices
    * containing a child node.
    *
    * {{{
    * //
    * //         pr = 0    -----
    * //  0 ------------> |  1  |  index 0
    * //    \             |     |
    * //     \            +-----+
    * //      \  pr = 1
    * //       --------->   (/)
    * }}}
    * This is the same setup as the full example where a full decision tree model is constructed.
    *
    * @param featureName the feature name to test
    * @param input an o
    */
  private[this] def testRandomDistGoingToNthIndex(featureName: String, input: Option[Int], missingOk: Boolean = true) {
    val acc = GeneratedAccessor(featureName, identity[Option[Int]])

    // Spec is as it is just to be consistent with the spec in the Semantics.
    val gaf = GenFunc.f1(acc)("${"+featureName+"} >= 0", _.map(_ < 1))

    // This container type must be a vector to make the error message work out.  This is because we are using the
    // same error message for manually constructed nodes and for nodes constructed by a decision tree that is parsed
    // from JSON, given the semantics
    val features = Vector(gaf)
    val selectInd1 = RandomNodeSelector(features, HashedCategoricalDistribution(0, 1), missingOk)
    val node = InteriorNode(0, immutable.IndexedSeq(Leaf(1)), selectInd1)

    val current = node.getNode(input)
    assertTrue(current.isLeft)

    val intNodeRes = current.left.get

    assertEquals("errors", errMsgs(featureName, missingOk), intNodeRes.errors)

    val missing = if (input.isEmpty) List(featureName) else Nil
    assertEquals("missing", missing, intNodeRes.missing)
    assertEquals("node", node, intNodeRes.node)
  }

  private[this] def errMsgs(featureName: String, missingOk: Boolean) =
    Seq(s"Node selector returned index = 1. Number of children = 1.  node selector: ${nodeSelectorToString(featureName, missingOk)}")

  private[this] def nodeSelectorToString(featureName: String, missingOk: Boolean) =
    "RandomNodeSelector(Vector(GenAggFunc((${" + featureName + "}) => ${" + featureName + "} >= 0)),<function1>," + missingOk + ")"
}

private object NodeTest {
  val FeatureName = "120oubnqgwe"
  val Features = Map(FeatureName -> 1.0)

  /** A basic semantics used for testing.
    */
  val Semantics = new Semantics[Map[String, Double]] {
    def refInfoA: RefInfo[Map[String, Double]] = RefInfo[Map[String, Double]]
    def close(): Unit = {}
    def accessorFunctionNames: Seq[String] = Nil
    def createFunction[B: RefInfo](codeSpec: String, default: Option[B]): Either[Seq[String], GenAggFunc[Map[String, Double], B]] = {
      val cs = codeSpec.trim
      val ga = GeneratedAccessor(cs, (_: Map[String, Double]).get(cs), Option("""(_: Map[String, Double]).get(cs)"""))
      val f = GenFunc.f1(ga)("${"+cs+"} >= 0", _ map {_ >= 0})
      val g = Right(f.asInstanceOf[GenAggFunc[Map[String, Double], B]])
      g
    }
  }

  /** This decision tree model has two nodes.  The node selector at the root node randomly selects the child index of
    * 1 with probability 1.  That is, it always selects index 1.  Since the root only has one child, at index 0, the
    * index returned by the node selector is outside the range of the children.  The job of the tests is to ensure
    * that no exceptions are thrown and that the score returns the appropriate information about the problem.
    * @param featureName the name of the feature used for randomization.  The name shouldn't matter.
    * @param returnBest  whether to return the best possible value in the decision tree.  If false, don't return
    *                    sub-par results, but instead just indicate failure.
    * @param missingDataOk whether it's OK to have missing data.
    * @return
    */
  private def modelFromJson(featureName: String, returnBest: Boolean = true, missingDataOk: Boolean = true) = {
    val json =
      s"""
         |{
         |  "modelType": "DecisionTree",
         |  "modelId": { "id": 0, "name": "" },
         |  "returnBest": $returnBest,
         |  "missingDataOk": $missingDataOk,
         |  "nodes": [
         |    {
         |      "id": 0,
         |      "value": 0,
         |      "selector": {
         |        "selectorType": "random",
         |        "children": [1],
         |        "features": ["$featureName"],
         |        "probabilities": [0, 1],
         |        "missingOk": $missingDataOk }
         |    },
         |    { "id": 1, "value": 1 }
         |  ]
         |}
            """.stripMargin

    val m = Factory.fromString(json).get
    m
  }

  private val Factory = NewModelFactory.defaultFactory(Semantics, TreeAuditor[Int]())
}
